/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: MySQLLotteryBatchDAOTest.java 120 2011-05-28 08:11:59Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 8, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.lottery.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * A class representing
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 120 $ $Date: 2011-05-28 03:11:59 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class MySQLLotteryBatchDAOTest {

	private Connection conn;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");

		conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/lotto?autoReconnect=true", "lotto", "lottopass");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		conn.close();
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#deleteLotteryBatch(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testDeleteLotteryBatch() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch);

		assertEquals(count, 1);

		boolean deleted = dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(deleted, true);

		batch = dao.findLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(null, batch);
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#findLotteryBatch(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testFindLotteryBatch() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch);

		batch = null;

		assertEquals(count, 1);

		batch = dao.findLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(200, batch.getPrice());

		boolean deleted = dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(deleted, true);

		assertEquals(batch.getPrice(), 200);
		assertEquals(batch.getFile(), "/Users/mloukianov/batch.lot");
		assertEquals(batch.getStatus(), "ON_HOLD");
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#insertLotteryBatch(com.ninelinelabs.server.lottery.dao.LotteryBatch)}.
	 */
	@Test
	public final void testInsertLotteryBatch() throws SQLException {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch);

		assertEquals(count, 1);

		batch = dao.findLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		if (batch == null) {
			fail("findLotteryBatch returned null; batch not found");
		}

		boolean deleted = dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(deleted, true);

		assertEquals(batch.getPrice(), 200);
		assertEquals(batch.getFile(), "/Users/mloukianov/batch.lot");
		assertEquals(batch.getStatus(), "ON_HOLD");
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#updateLotteryBatch(com.ninelinelabs.server.lottery.dao.LotteryBatch)}.
	 */
	@Test
	public final void testUpdateLotteryBatch() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch);

		assertEquals(count, 1);

		LotteryBatch newBatch = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 500, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		count = dao.updateLotteryBatch(newBatch);

		assertEquals(1, count);

		LotteryBatch resBatch = dao.findLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		if (resBatch == null) {
			fail ("findLotteryBatch returned null; batch not found");
		}

		assertEquals(500, resBatch.getPrice());

		boolean deleted = dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals(deleted, true);
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#listByStatus(java.lang.String)}.
	 */
	@Test
	public final void testListByStatus() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch1 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch1);

		assertEquals(count, 1);

		LotteryBatch batch2 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890100", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch2);

		assertEquals(count, 1);

		LotteryBatch batch3 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890105", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch3);

		assertEquals(count, 1);

		ArrayList<LotteryBatch> list1 = dao.listByStatus("ON_HOLD");

		assertEquals(1, list1.size());

		ArrayList<LotteryBatch> list2 = dao.listByStatus("ACTIVE");

		assertEquals(2, list2.size());

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890100");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890105");
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#listAll()}.
	 */
	@Test
	public final void testListAll() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch1 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch1);

		assertEquals(count, 1);

		LotteryBatch batch2 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890100", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch2);

		assertEquals(count, 1);

		LotteryBatch batch3 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890105", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch3);

		assertEquals(count, 1);

		ArrayList<LotteryBatch> list1 = dao.listAll();

		assertEquals(3, list1.size());

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890100");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890105");
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.dao.mysql.MySQLLotteryBatchDAO#setLastTicket(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testSetLastTicket() {
		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);

		LotteryBatchDAO dao = daoFactory.getLotteryBatchDAO(conn);

		LotteryBatch batch1 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890123", 200, "/Users/mloukianov/batch.lot", "", "ON_HOLD");

		int count = dao.insertLotteryBatch(batch1);

		assertEquals(count, 1);

		LotteryBatch batch2 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890100", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch2);

		assertEquals(count, 1);

		LotteryBatch batch3 = new LotteryBatch("LOT_1234567890", "BATCH_1234567890105", 200, "/Users/mloukianov/batch.lot", "", "ACTIVE");

		count = dao.insertLotteryBatch(batch3);

		assertEquals(count, 1);

		count = dao.setLastTicket("LOT_1234567890", "BATCH_1234567890123", "TIX122823411");

		LotteryBatch batch = dao.findLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		assertEquals("TIX122823411", batch.getTicket());

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890123");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890100");

		dao.deleteLotteryBatch("LOT_1234567890", "BATCH_1234567890105");
	}

}
