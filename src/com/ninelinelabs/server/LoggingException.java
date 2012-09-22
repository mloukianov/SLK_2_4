/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LoggingException.java 72 2011-05-20 01:35:38Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

/**
 * An class representing logging exception.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 72 $ $Date: 2011-05-19 20:35:38 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class LoggingException extends Exception {

	private static final long serialVersionUID = 4607114677521916507L;

	private int log;

	public static final int TRANSACTION_LOG = 1;
	public static final int LOTTERY_LOG = 2;
	public static final int SECURITY_LOG = 3;

	/**
	 *
	 */
	public LoggingException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public LoggingException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public LoggingException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public LoggingException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public LoggingException(String message, int log) {
		super(message);
		this.log = log;
	}

	public LoggingException(int log) {
		super();
		this.log = log;
	}

	public LoggingException(int log, Throwable cause) {
		super(cause);
		this.log = log;
	}

	public int getLog() {
		return this.log;
	}
}
