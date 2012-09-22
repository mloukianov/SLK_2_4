/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TicketLoader.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 01, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings 
 *
 */
package com.ninelinelabs.server.lottery;

import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;

import com.ninelinelabs.lottery.generator.vo.*;

import com.ninelinelabs.server.lottery.dao.LotteryBatch;

/**
 * A class representing ticket loader to be used for loading tickets from file.
 *
 * Ticket file format:
 * HEADER
 * lottery ID
 * batch ID
 * ticket price
 * number of tickets
 *
 * TICKET
 * ticket id
 * ticket data
 *
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
public class TicketLoader implements Runnable {

	private final String filename;
	private final LinkedBlockingQueue<LongTicketVO> queue;

	private String lottery;
	private String batch;
	private int price;
	@SuppressWarnings("unused")
	private int number;

	private DataInputStream is;

	/**
	 * Creates instance of TicketLoader associated with specific file and queue
	 *
	 * @param filename   name of the file containing batch of lottery tickets
	 * @param queue      queue to send tickets into
	 */
	public TicketLoader(String filename, LinkedBlockingQueue<LongTicketVO> queue) {
		this.filename = filename;
		this.queue = queue;
	}

	public LinkedBlockingQueue<LongTicketVO> getQueue() {
		return this.queue;
	}

	public LotteryBatch parse() throws IOException {
		is = new DataInputStream(new FileInputStream(filename));

		lottery = is.readUTF();
		batch = is.readUTF();
		price = is.readInt();

		return new LotteryBatch(lottery, batch, price, filename, "", "ACTIVE");
	}

	public void rewind(String ticket) throws IOException {
		String current = "";

		if (ticket.equals("")) return;

		// TODO: what happens if ticket is not found in the batch? we block here
		while (!(current = is.readUTF()).equals(ticket)) {
			System.out.println("Rewinding: reading ticket " + current);
			@SuppressWarnings("unused")
			LongTicketVO vo = LongTicketVO.readTicket(is);
		}
	}

	public LongTicketVO getNextTicket() throws IOException {
		String ticketNo = is.readUTF();
		return LongTicketVO.readTicket(is).setTicketNo(ticketNo);
	}

	/**
	 * Open the file and parse header record
	 */
	public void init() throws IOException {
		is = new DataInputStream(new FileInputStream(filename));

		lottery = is.readUTF();
		batch = is.readUTF();
		price = is.readInt();
		number = is.readInt();
	}

	/**
	 * Closes the file and stops parsing
	 */
	public void close() {

	}


	public void run() {
		try {
			while (true) {
				LongTicketVO vo = this.getNextTicket();
				queue.put(vo);
				System.out.println("Served ticket " + vo.getTicketNo() + " to the queue for " + filename);
			}
		}
		catch(IOException ioe) {

		}
		catch(InterruptedException ie) {

		}
	}
}
