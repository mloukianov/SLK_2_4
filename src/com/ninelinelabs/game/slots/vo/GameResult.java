/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: GameResult.java $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.game.slots.vo;

/**
 * An class representing game outcome.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision:  $ $Date:  $
 * @see
 */
public class GameResult {
	private GameStops stops;
	private int win;
	private String type;

	public static final String FREE_GAMES = "FREE_GAMES";
	public static final String BONUS_GAME = "BONUS_GAME";
	public static final String FINAL_WIN  = "FINAL_WIN";

	public GameResult(GameStops stops, int win, String type) {
		this.stops = stops;
		this.win = win;
		this.type = type;
	}

	public GameStops getStops() {
		return stops;
	}

	public int getWin() {
		return win;
	}

	public String getType() {
		return type;
	}
}
