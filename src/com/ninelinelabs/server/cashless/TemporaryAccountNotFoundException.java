/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TemporaryAccountNotFoundException.java 118 2011-05-28 07:28:25Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Feb 03, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * 
 */
package com.ninelinelabs.server.cashless;

/**
 * An class representing exception being thrown when temporary account not found
 * (when removing temporary account).
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 118 $ $Date: 2011-05-28 02:28:25 -0500 (Sat, 28 May 2011) $
 * @see 
 */
public class TemporaryAccountNotFoundException extends Throwable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 372199446141793554L;
	private final String account;

	/**
	 * Creates exception given the account number
	 * 
	 * @param account  account number
	 */
	public TemporaryAccountNotFoundException(String account) {
		this.account = account;
	}

	/**
	 * Creates exception given the account number and comment string
	 * 
	 * @param account  account number
	 * @param arg0     comment string
	 */
	public TemporaryAccountNotFoundException(String account, String arg0) {
		super(arg0);
		this.account = account;	
	}

	/**
	 * Creates exception given the account number and root cause exception
	 * 
	 * @param account  account number
	 * @param arg0     root cause exception
	 */
	public TemporaryAccountNotFoundException(String account, Throwable arg0) {
		super(arg0);
		this.account = account;	
	}

	/**
	 * Creates exception given the account number, comment string, and root cause exception
	 * 
	 * @param account  account number
	 * @param arg0     comment string
	 * @param arg1     root cause exception
	 */
	public TemporaryAccountNotFoundException(String account, String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.account = account;
	}

	/**
	 * Returns account number
	 * 
	 * @return account number
	 */
	public String getAccount() {
		return this.account;
	}
}
