/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LastTicketLogger.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 04, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.lottery;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import java.sql.Connection;
import java.sql.SQLException;

import com.ninelinelabs.lottery.generator.vo.LongTicketVO;
import com.ninelinelabs.server.lottery.dao.DAOFactory;
import com.ninelinelabs.server.lottery.dao.LotteryBatchDAO;

/**
 * Used to log last ticket in the lottery batch.
 * NOTE: Making it a singleton might be not such a good idea after all.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class LastTicketLogger {

	// private static LastTicketLogger INSTANCE = new LastTicketLogger();
	@SuppressWarnings("unused")
	private String lottery;
	@SuppressWarnings("unused")
	private String batch;
	@SuppressWarnings("unused")
	private LinkedBlockingQueue<LongTicketVO> queue;
	@SuppressWarnings("unused")
	private SynchronousQueue<LongTicketVO> synchronizedQueue;

	private Connection conn;

	private LotteryBatchDAO dao;

	public LastTicketLogger(String lottery, String batch, LinkedBlockingQueue<LongTicketVO> queue, SynchronousQueue<LongTicketVO> synchronizedQueue) {
		this.lottery = lottery;
		this.batch = batch;
		this.queue = queue;
		this.synchronizedQueue = synchronizedQueue;
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
	}


	/**
	 * Logs last ticket from the batch in the database
	 *
	 * @param lottery   lottery id string
	 * @param batch     batch id string
	 * @param ticket    last ticket id string
	 */
	public synchronized void logLastTicket(String lottery, String batch, String ticket) throws SQLException {
		// TODO: we might be able to get away without using synchronized method
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		dao = daoFactory.getLotteryBatchDAO(conn);

		dao.setLastTicket(lottery, batch, ticket);

		conn.commit();

	}


}
