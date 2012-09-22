/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LotteryGameResult.java 239 2011-09-08 03:15:29Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Aug 28, 2011 mloukianov Changed javadoc class description
 *
 */
package com.ninelinelabs.game.slots.vo;

/**
 * Lottery gameplay outcome
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 239 $ $Date: 2011-09-07 22:15:29 -0500 (Wed, 07 Sep 2011) $
 * @see
 */
public class LotteryGameResult extends GameResult {
	private String ticket;
	private int next;

	public LotteryGameResult(GameStops stops, int win, String type, String ticket) {
		super(stops, win, type);
		this.ticket = ticket;
	}

	public String getTicket() {
		return ticket;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public int getNext() {
		return this.next;
	}
}
