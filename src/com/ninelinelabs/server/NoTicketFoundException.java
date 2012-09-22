/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: NoTicketFoundException.java 76 2011-05-20 01:40:27Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

/**
 * An class representing no ticket found exception.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 76 $ $Date: 2011-05-19 20:40:27 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class NoTicketFoundException extends Exception {

	private static final long serialVersionUID = 4957582523423168058L;

	private int ticket;

	/**
	 *
	 */
	public NoTicketFoundException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public NoTicketFoundException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public NoTicketFoundException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NoTicketFoundException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoTicketFoundException(String message, int ticket) {
		super(message);
		this.ticket = ticket;
	}

	public NoTicketFoundException(int ticket) {
		super();
		this.ticket = ticket;
	}

	public int getTicket() {
		return this.ticket;
	}
}
