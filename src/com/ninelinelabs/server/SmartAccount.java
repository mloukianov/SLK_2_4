/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SmartAccount.java 96 2011-05-28 07:14:45Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * A class representing smart card account.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 96 $ $Date: 2011-05-28 02:14:45 -0500 (Sat, 28 May 2011) $
 * @see
 */public class SmartAccount {
	private String cardNumber;
	private int amount;

	public SmartAccount(String cardNumber) {
		this.cardNumber = cardNumber;
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

	public String getCardNumber() {
		return this.cardNumber;
	}

}
