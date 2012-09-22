/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: PinAccount.java 79 2011-05-20 01:49:11Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * An class representing pin account.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 79 $ $Date: 2011-05-19 20:49:11 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class PinAccount {
	private String pin;
	private int amount = 0;

	public PinAccount(String pin) {
		this.pin = pin;
	}

	public int addMoney(int amount) {
		this.amount += amount;
		return this.amount;
	}

	public int removeMoney(int amount) {
		this.amount -= amount;
		return this.amount;
	}

	public int getMoney() {
		return this.amount;
	}

	public String getPin() {
		return this.pin;
	}

}
