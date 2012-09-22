/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: MySQLLotteryBatchDAO.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 07, 2009 mloukianov Created
 * Oct 14, 2009 mloukianov Refactoring
 *
 */
package com.ninelinelabs.server.lottery.dao.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;

import com.ninelinelabs.server.lottery.dao.LotteryBatch;
import com.ninelinelabs.server.lottery.dao.LotteryBatchDAO;

/**
 * Lottery batch DAO implementation for MySQL database.
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
public class MySQLLotteryBatchDAO implements LotteryBatchDAO {
	private final Connection conn;

	public static final Logger logger = Logger.getLogger(MySQLLotteryBatchDAO.class.getName());

	public MySQLLotteryBatchDAO(Connection conn) {
		this.conn = conn;
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.lottery.dao.LotteryBatchDAO#deleteLotteryBatch(java.lang.String, java.lang.String)
	 */
	public boolean deleteLotteryBatch(String lottery, String batch) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("DELETE FROM TICKET_BATCH WHERE LOTTERY_ID = ? AND BATCH_ID = ?");

			ps.setString(1, lottery);
			ps.setString(2, batch);


			if (ps.executeUpdate() == 1) {
				return true;
			} else {
				return false;
			}
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception removing lottery batch [" + lottery + ", " + batch + "]", sqle);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.lottery.dao.LotteryBatchDAO#findLotteryBatch(java.lang.String, java.lang.String)
	 */
	public LotteryBatch findLotteryBatch(String lottery, String batch) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("SELECT LOTTERY_ID, BATCH_ID, TICKET_PRICE, TICKETS_FILE, LAST_TICKET, STATUS FROM TICKET_BATCH WHERE LOTTERY_ID = ? AND BATCH_ID = ?");

			ps.setString(1, lottery);
			ps.setString(2, batch);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				LotteryBatch result = new LotteryBatch(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6));
				rs.close();
				return result;
			} else {
				return null;
			}
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception retrieving lottery batch [" + lottery + ", " + batch + "]", sqle);
			return null;
		}
	}

	public LotteryBatch findByFilename(String filename) {

		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("SELECT LOTTERY_ID, BATCH_ID, TICKET_PRICE, TICKETS_FILE, LAST_TICKET, STATUS FROM TICKET_BATCH WHERE TICKETS_FILE = ?");

			ps.setString(1, filename);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				LotteryBatch result = new LotteryBatch(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6));
				rs.close();
				return result;
			} else {
				return null;
			}
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception retrieving lottery batch [" + filename + "]", sqle);
			return null;
		}
	}

	
	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.lottery.dao.LotteryBatchDAO#insertLotteryBatch(com.ninelinelabs.server.lottery.dao.LotteryBatch)
	 */
	public int insertLotteryBatch(LotteryBatch batch) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("INSERT INTO TICKET_BATCH(LOTTERY_ID, BATCH_ID, TICKET_PRICE, TICKETS_FILE, LAST_TICKET, STATUS) VALUES(?, ?, ?, ?, ?, ?)");

			ps.setString(1, batch.getLottery());
			ps.setString(2, batch.getBatch());
			ps.setInt(3, batch.getPrice());
			ps.setString(4, batch.getFile());
			ps.setString(5, batch.getTicket());
			ps.setString(6, batch.getStatus());

			int result = ps.executeUpdate();

			return result;
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception inserting lottery batch [" + batch.getLottery() + ", " + batch.getBatch() + "]", sqle);
			return -1;
		}
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.lottery.dao.LotteryBatchDAO#updateLotteryBatch(com.ninelinelabs.server.lottery.dao.LotteryBatch)
	 */
	public int updateLotteryBatch(LotteryBatch batch) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("UPDATE TICKET_BATCH SET TICKET_PRICE = ?, TICKETS_FILE = ?, LAST_TICKET =?, STATUS =? WHERE LOTTERY_ID = ? AND BATCH_ID = ?");

			ps.setInt(1, batch.getPrice());
			ps.setString(2, batch.getFile());
			ps.setString(3, batch.getTicket());
			ps.setString(4, batch.getStatus());
			ps.setString(5, batch.getLottery());
			ps.setString(6, batch.getBatch());

			return ps.executeUpdate();
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception updating lottery batch [" + batch.getLottery() + ", " + batch.getBatch() + "]", sqle);
			return -1;
		}
	}

	public ArrayList<LotteryBatch> listByStatus(String status) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("SELECT LOTTERY_ID, BATCH_ID, TICKET_PRICE, TICKETS_FILE, LAST_TICKET, STATUS FROM TICKET_BATCH WHERE STATUS = ?");

			ps.setString(1, status);

			ResultSet rs = ps.executeQuery();

			ArrayList<LotteryBatch> result = new ArrayList<LotteryBatch>();

			while (rs.next()) {
				LotteryBatch batch = new LotteryBatch(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6));
				result.add(batch);
			}

			rs.close();
			return result;
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception retrieving lottery batches: [ state = " + status + "]", sqle);
			return null;
		}
	}

	public ArrayList<LotteryBatch> listAll() {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("SELECT LOTTERY_ID, BATCH_ID, TICKET_PRICE, TICKETS_FILE, LAST_TICKET, STATUS FROM TICKET_BATCH");

			ResultSet rs = ps.executeQuery();

			ArrayList<LotteryBatch> result = new ArrayList<LotteryBatch>();

			while (rs.next()) {
				LotteryBatch batch = new LotteryBatch(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6));
				result.add(batch);
			}

			rs.close();
			return result;
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception retrieving all lottery batches", sqle);
			return null;
		}
	}

	public int setLastTicket(String lottery, String batch, String ticket) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("UPDATE TICKET_BATCH SET LAST_TICKET =? WHERE LOTTERY_ID = ? AND BATCH_ID = ?");

			ps.setString(1, ticket);
			ps.setString(2, lottery);
			ps.setString(3, batch);

			return ps.executeUpdate();
		}
		catch(SQLException sqle) {
			logger.log(Level.SEVERE, "Exception updating last ticket (" + ticket + ") in batch [" + lottery + ", " + batch + "]", sqle);
			return -1;
		}
	}
}
