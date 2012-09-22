/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SpecialCardWin.java 99 2011-05-28 07:16:14Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 */
package com.ninelinelabs.server;

public class SpecialCardWin {
	private int bet;
	private int lines;
	private int win;

	public SpecialCardWin(int lines, int bet, int win) {
		this.lines = lines;
		this.bet = bet;
		this.win = win;
	}

	public int getLines() {
		return this.lines;
	}

	public int getBet() {
		return this.bet;
	}

	public int getWin() {
		return this.win;
	}
}
