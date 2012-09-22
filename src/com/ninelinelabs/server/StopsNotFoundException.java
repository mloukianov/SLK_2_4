/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: StopsNotFoundException.java 100 2011-05-28 07:16:38Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

/**
 * A class representing stops not found exception.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 100 $ $Date: 2011-05-28 02:16:38 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class StopsNotFoundException extends Exception {

	private static final long serialVersionUID = -8390359214982477434L;

	private int win;

	/**
	 *
	 */
	public StopsNotFoundException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public StopsNotFoundException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public StopsNotFoundException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public StopsNotFoundException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public StopsNotFoundException(String message, int win) {
		super(message);
		this.win = win;
	}

	public StopsNotFoundException(int win) {
		super();
		this.win = win;
	}

	public int getWin() {
		return this.win;
	}
}
