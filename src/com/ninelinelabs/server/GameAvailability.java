/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: $
 *
 * Date Author Changes
 * July 01, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Game availability on the server
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 69 $ $Date: 2011-05-19 20:31:31 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class GameAvailability {
	private String game;
	private String lottery;
	private int price;
	private boolean available;

	public GameAvailability(String game, String lottery, int price, boolean available) {
		this.game = game;
		this.lottery = lottery;
		this.price = price;
		this.available = available;
	}

	public String getGame() {
		return this.game;
	}

	public String getLottery() {
		return this.lottery;
	}

	public int getPrice() {
		return this.price;
	}

	public boolean available() {
		return this.available;
	}
}
