/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: AccountConsolidationException.java $
 *
 * Date Author Changes
 * Mar 30, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.cashless;

/**
 * Exception representing account consolidation error.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 * @see
 */
public class AccountConsolidationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3169681127012833286L;

	/**
	 *
	 */
	public AccountConsolidationException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public AccountConsolidationException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public AccountConsolidationException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public AccountConsolidationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
