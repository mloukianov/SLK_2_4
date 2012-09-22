/*
 * Copyright (C) 2008-2011, Nine Line Labs, LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs, LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TransactionException.java 118 2011-05-28 07:28:25Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 14, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.cashless;

/**
 * Exception thrown when something goes wrong in transaction processing
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 118 $ $Date: 2011-05-28 02:28:25 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class TransactionException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3028433719200902217L;
	public static final String ACCOUNT_NOT_FOUND = "ACCOUNT NOT FOUND; ACCOUNT NO: ";

	/**
	 *
	 */
	public TransactionException() {
	}

	/**
	 * @param arg0
	 */
	public TransactionException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public TransactionException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public TransactionException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
