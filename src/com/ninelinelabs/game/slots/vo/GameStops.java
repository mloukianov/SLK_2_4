/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: GameStops.java 238 2011-09-08 03:13:56Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Aug 28, 2011 mloukianov Changed javadoc class description
 *
 */
package com.ninelinelabs.game.slots.vo;

/**
 * Game stops for slot game
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 238 $ $Date: 2011-09-07 22:13:56 -0500 (Wed, 07 Sep 2011) $
 * @see
 */
public class GameStops {
	private int[] stops;

	public GameStops(int lines) {
		stops = new int[lines];
	}

	public int[] getStops() {
		return stops;
	}

	public void setStops(int[] stops) {
		this.stops = stops;
	}

	public void setStop(int line, int stop) {
		stops[line - 1] = stop;
	}
}
