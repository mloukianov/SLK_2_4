/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TicketServiceTest.java 120 2011-05-28 08:11:59Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 11, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.lottery;

import static org.junit.Assert.*;

import java.sql.DriverManager;
import java.sql.Connection;

import java.io.IOException;

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
public class TicketServiceTest {
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
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.TicketService#init()}.
	 */
	@Test
	public void testInit() {
		try {
			TicketService service = new TicketService("/Users/mloukianov/Desktop/lotto");
			service.setConnection(conn);
			service.init();
		} catch (InterruptedException ie) {
			fail("Failed w/ interrupted exception");
		} catch(IOException e) {
			fail("Failed with IOException");
			e.printStackTrace();
		}
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.TicketService#start()}.
	 */
	@Test
	public void testStart() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.TicketService#stop()}.
	 */
	@Test
	public void testStop() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link com.ninelinelabs.server.lottery.TicketService#destroy()}.
	 */
	@Test
	public void testDestroy() {
		fail("Not yet implemented"); // TODO
	}

}
