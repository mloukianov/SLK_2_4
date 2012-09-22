/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: WrongBanknoteException.java 111 2011-05-28 07:20:18Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * Mar 07, 2010 mloukianov Added amount parameter to track wrong bank note denominations
 *
 */
package com.ninelinelabs.server;

public class WrongBanknoteException extends Throwable {

	private static final long serialVersionUID = -7669056000189916415L;

	private int amount;

	public WrongBanknoteException() {
	}

	public WrongBanknoteException(String message) {
		super(message);
	}

	public WrongBanknoteException(Throwable cause) {
		super(cause);
	}

	public WrongBanknoteException(String message, Throwable cause) {
		super(message, cause);
	}

	public WrongBanknoteException(int amount) {
		this.amount = amount;
	}

	public int getAmount() {
		return this.amount;
	}

}
