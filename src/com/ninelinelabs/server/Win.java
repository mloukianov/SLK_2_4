/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Win.java 109 2011-05-28 07:19:47Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * An class representing winning outcome for the game.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 109 $ $Date: 2011-05-28 02:19:47 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class Win {
	private int amount;
	private int[] stops;

	public Win(int amount, int[] stops) {
		this.amount = amount;
		this.stops = stops;
	}

	public int getAmount() {
		return this.amount;
	}

	public int[] getStops() {
		return this.stops;
	}

}
