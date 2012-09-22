/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: RequestProcessorTest.java 142 2011-06-22 14:56:30Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 8, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.test;

import static org.junit.Assert.*;

import org.apache.commons.dbcp.BasicDataSource;

import org.dbunit.Assertion;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ninelinelabs.server.BanknoteCount;
import com.ninelinelabs.server.DepositRequestResult;
import com.ninelinelabs.server.LoggingException;
import com.ninelinelabs.server.RequestProcessor;
import com.ninelinelabs.server.cashless.AccountNotFoundException;
import com.ninelinelabs.server.cashless.TransactionException;
import com.ninelinelabs.server.processor.DepositRequestProcessor;
import com.ninelinelabs.util.database.ConnectionDispenser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RequestProcessorTest {

	private IDatabaseTester databaseTester;

	private String terminal;

	public static final Logger logger = Logger.getLogger(RequestProcessorTest.class.getName());

	@Before
	public void setUp() throws Exception {

		databaseTester = new JdbcDatabaseTester("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/maxlotto?autoReconnect=true", "maxlotto", "lottopass");

		IDataSet dataSet = new FlatXmlDataSet(new File("test/accounts-init.xml"));

		databaseTester.setDataSet(dataSet);


		databaseTester.onSetup();

		BasicDataSource ds = new BasicDataSource();

		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUsername("maxlotto");
		ds.setPassword("lottopass");
		ds.setUrl("jdbc:mysql://localhost:3306/maxlotto?autoReconnect=true");
		ds.setMaxActive(10);
		ds.setMaxIdle(5);
		ds.setInitialSize(5);
		ds.setValidationQuery("SELECT 1");

		ConnectionDispenser.setDataSource(ds);

		terminal = "T0001";
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testProcessDepositBNARequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessConnectionLost() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessExitLotteryRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessBuyLongTicketRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBankAccountBalance() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessBuyTicketRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessCashoutRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessDepositRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRoleForCard() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddTLogRecord() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessAuthenticationPINRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessPlayLongTicketRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessPlayRequest() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBanknoteCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testRecordEndCount() {
		fail("Not yet implemented");
	}


	@Test
	public void testThatProcessDepositRequestWorks() throws Exception {

		String game = "GBOR0122";
		String ticket = "011234567";
		int playseq = 2;

		DepositRequestResult result = DepositRequestProcessor.processDepositRequest(terminal, game, ticket, playseq);

		assertTrue("processing message result must be true", result.success());

		IDataSet dataSet = new FlatXmlDataSet(new File("test/accounts-done.xml"));

		IDatabaseConnection connection = databaseTester.getConnection();

		IDataSet actualDataSet = connection.createDataSet();

		Assertion.assertEquals(dataSet, actualDataSet);

	}


	@Test
	public void testPerformTerminalCount() {
		fail("Not yet implemented");
	}

}
