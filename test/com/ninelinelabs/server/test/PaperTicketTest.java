/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: PaperTicketTest.java 187 2011-06-23 12:18:09Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 8, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.test;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ninelinelabs.server.PaperTicket;

public class PaperTicketTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParseTicket() {

		PaperTicket ticket = PaperTicket.parseTicket("330010110075");

		System.out.println("lottery: " + ticket.getLottery());
		System.out.println("series: " + ticket.getSeries());
		System.out.println("ticket: " + ticket.getTicket());
		System.out.println("ticket prefix: " + ticket.getTicketprefix());
		System.out.println("ticket no: " + ticket.getTicketno());
		System.out.println("price: " + ticket.getPrice());

		Assert.assertEquals("33", ticket.getLottery());
		Assert.assertEquals("001", ticket.getSeries());
		Assert.assertEquals("0110040", ticket.getTicket());
		Assert.assertEquals("011", ticket.getTicketprefix());
		Assert.assertEquals(new Integer(40), new Integer(ticket.getTicketno()));
	}

}
