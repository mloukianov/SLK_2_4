/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Balance.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Sep 16, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.server.model;

public class Balance {
	private int bank;
	private int credits;
	private int win;
	
	public Balance(int bank, int credits, int win) {
		this.bank = bank;
		this.credits = credits;
		this.win = win;
	}
	
	public int getBank() {
		return this.bank;
	}
	
	public int getCredits() {
		return this.credits;
	}
	
	public int getWin() {
		return this.win;
	}
	
	public int increaseBank(int amount) {
		bank += amount;
		return bank;
	}
	
	public int decreaseBank(int amount) {
		bank -= amount;
		return bank;
	}
	
	public int increaseCredits(int amount) {
		credits += amount;
		return credits;
	}
	
	public int decreaseCredits(int amount) {
		credits -= amount;
		return credits;
	}
	
	public int increaseWin(int amount) {
		win += amount;
		return win;
	}
	
	public int decreaseWin(int amount) {
		win -= amount;
		return win;
	}
	
	public String toString() {
		return "Balance: [bank : " + bank + "; credits : " + credits + "; win : " + win + " ]";
	}
}
