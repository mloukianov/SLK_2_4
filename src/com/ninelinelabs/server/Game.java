/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Game.java 68 2011-05-20 01:30:53Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Class representing game.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 68 $ $Date: 2011-05-19 20:30:53 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class Game {

	public static final int GAME_NONE = 1;
	public static final int GAME_LOBBY = 2;

	public static final int GAME_NORMAL_INITIAL = 3;
	public static final int GAME_NORMAL_BET = 4;
	public static final int GAME_NORMAL_WIN = 5;

	public static final int GAME_DOUBLEUP = 6;
	public static final int GAME_DOUBLEUP_WIN = 7;

	public static final int GAME_FREE_INITIAL = 10;
	public static final int GAME_FREE_BET = 11;
	public static final int GAME_FREE_WIN = 12;

	public static final int GAME_CHOICE_INITIAL = 20;
	public static final int GAME_CHOICE_WIN = 21;

	public static final int GAME_TECH = 99;

	private String id;
	private int escrow = 0;
	private int gameProceeds = 0;

	public Game(String gameId) {
		this.id = gameId;
	}

	public String getId() {
		return id;
	}

	public Win bet(int betAmount) {
		int[] stops = new int[] {1, 12, 22, 3, 22};

		return new Win(153, stops);
	}

	public int getEscrow() {
		return this.escrow;
	}

	public void putEscrow(int credits) {
		this.escrow += credits;
	}

	public int removeEscrow() {
		int result = this.escrow;
		this.escrow = 0;
		return result;
	}

	public void addGameProceeds(int amount) {
		this.gameProceeds += amount;
	}

	public int getGameProceeds() {
		return this.gameProceeds;
	}
}
