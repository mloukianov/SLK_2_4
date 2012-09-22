/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: NoLotteryTicketFoundException.java 75 2011-05-20 01:38:39Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

/**
 * An class representing no lottery ticket found exception.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 75 $ $Date: 2011-05-19 20:38:39 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class NoLotteryTicketFoundException extends Exception {

	private static final long serialVersionUID = 6063256101016553801L;

	private String game;

	public NoLotteryTicketFoundException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NoLotteryTicketFoundException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoLotteryTicketFoundException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public NoLotteryTicketFoundException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoLotteryTicketFoundException(String message, String game) {
		super(message);
		this.game = game;
	}

	public String getGame() {
		return this.game;
	}
}
