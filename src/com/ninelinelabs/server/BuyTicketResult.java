/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: BuyTicketResult.java 234 2011-09-08 03:06:24Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Jul 01, 2009 mloukianov Created
 * Aug 28, 2011 mloukianov Added doublesale field to indicate this ticket was sold twice on the terminal
 *
 */
package com.ninelinelabs.server;

import com.ninelinelabs.lottery.generator.vo.bor.BorLongTicketVO;

/**
 * Result for buy ticket request
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 234 $ $Date: 2011-09-07 22:06:24 -0500 (Wed, 07 Sep 2011) $
 * @see
 */
public class BuyTicketResult {

	private int bank;
	private int credits;
	private BorLongTicketVO ticket;
	private boolean doublesale;

	public BuyTicketResult(BorLongTicketVO ticket, int bank, int credits) {
		this.ticket = ticket;
		this.bank = bank;
		this.credits = credits;
	}

	public BorLongTicketVO getTicket() {
		return this.ticket;
	}

	public int getBank() {
		return this.bank;
	}

	public int getCredits() {
		return this.credits;
	}

	public void setDoublesale(boolean doublesale) {
		this.doublesale = doublesale;
	}

	public boolean isDoublesale() {
		return this.doublesale;
	}
}
