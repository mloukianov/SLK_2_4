/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TicketService.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 04, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.lottery;

import java.io.File;
import java.io.IOException;

import java.sql.*;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.ninelinelabs.server.lottery.dao.*;
import com.ninelinelabs.lottery.generator.vo.*;

/**
 * A class representing a lottery tickets service.
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class TicketService {
	private final String dir;
	private Connection conn;

	private LotteryBatchDAO dao;

	private HashMap<String, TicketLoader> loaders = new HashMap<String, TicketLoader>();

	public static final int BUFFER_SIZE = 10;

	private String[] list = null;


	public TicketService(String dir) {
		this.dir = dir;
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
	}


	public void init() throws IOException, InterruptedException {

		// Get DAO factory and set up DAO
		// TODO: we need to make sure we are not using the same connection
		dao = DAOFactory.getDAOFactory(DAOFactory.MYSQL).getLotteryBatchDAO(conn);

		// List files in the directory
		// Lottery batches have the format *.lot
		list = new File(dir).list();

		// Process each file found in the directory
		for (int i = 0; i < list.length; i++) {
			String filename = list[i];
			if (filename.endsWith(".lot")) {
				// If it's a lottery file, look up file information in the database
				LotteryBatch batch = dao.findByFilename(dir + "/" + filename);
				if (batch == null) {
					// new batch file found; parse and create record as well as TicketLoader
					LinkedBlockingQueue<LongTicketVO> queue = new LinkedBlockingQueue<LongTicketVO>(TicketService.BUFFER_SIZE);
					TicketLoader loader = new TicketLoader(dir + "/" + filename, queue);
					batch = loader.parse();
					// Insert lottery batch information into the database
					dao.insertLotteryBatch(batch);
					loaders.put(filename, loader);
				} else {
					// old batch found; need to get last ticket processed and rewind
					LinkedBlockingQueue<LongTicketVO> queue = new LinkedBlockingQueue<LongTicketVO>(TicketService.BUFFER_SIZE);
					TicketLoader loader = new TicketLoader(dir + "/" + filename, queue);
					batch = loader.parse();
					// Rewind the batch to the last ticket sold
					loader.rewind(batch.getTicket());
					loaders.put(filename, loader);
				}
			}
		}

		// Load up the buffer
		// TODO: buffer might not even be needed since we are buffering the file itself
		for (int i = 0; i < list.length; i++) {
			String filename = list[i];
			if (filename.endsWith(".lot")) {
				TicketLoader loader = loaders.get(filename);
				LinkedBlockingQueue<LongTicketVO> queue = loader.getQueue();
				for (int j = 0; j < TicketService.BUFFER_SIZE; j++) {
					LongTicketVO vo = loader.getNextTicket();
					queue.put(vo);
					System.out.println("Added ticket " + vo.getTicketNo() + " to the buffer queue for " + filename);
				}
			}
		}

		// connect to the database
		// read information about all batches on the file system
		// read final ticket for each batch
		// create LinkedBlockingQueue<Ticket> instance for each batch
		// create singleton
		// create TicketLoader instance for each batch
		// initialize TicketLoader instance for each batch
		// run each TicketLoader to skip to the final ticket for each batch
		// run TicketLoader to load N ticket into the LickedBlockingQueue<Ticket> as a buffer
		// done
	}

	public void start() {
		for (int i = 0; i < list.length; i++) {
			if (list[i].endsWith(".lot")) {
				TicketLoader loader = loaders.get(list[i]);
				@SuppressWarnings("unused")
				LinkedBlockingQueue<LongTicketVO> queue = loader.getQueue();
				Thread thread = new Thread(loader);
				thread.setName("Thread-" + list[i]);
				thread.start();
			}
		}
	}

	public void stop() {

	}

	public void destroy() {

	}
}
