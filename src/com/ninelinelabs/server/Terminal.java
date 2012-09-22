/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Terminal.java 101 2011-05-28 07:16:56Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

import java.util.HashMap;

/**
 * A class representing video lottery terminal.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 101 $ $Date: 2011-05-28 02:16:56 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class Terminal {

	public static final String UNAVAILABLE_STATE = "UNAVAILABLE";
	public static final String IDLE_STATE = "IDLE";
	public static final String COUNT_STATE = "COUNT";
	public static final String INPLAY_STATE = "INPLAY";
	public static final String BLOCKED_STATE = "BLOCKED";
	public static final String TECH_STATE = "TECH";

	private String terminalId;
	private HashMap<String, Game> games = new HashMap<String, Game>();
	private PinAccount pinAccount;


	private int money = 0;
	private int denomination = 25;

	public Terminal(String terminalId) {
		this.terminalId = terminalId;
	}

	public String getId() {
		return this.terminalId;
	}

	public void addGame(Game game) {
		games.put(game.getId(), game);
	}

	public void removeGame(String gameId) {
		games.remove(gameId);
	}

	public Game getGame(String gameId) {
		return (Game)games.get(gameId);
	}


	public int addMoney(int money) {
		this.money += money;
		return this.money;
	}

	public int removeMoney(int money) {
		this.money -= money;
		return money;
	}

	public int getMoneyCount() {
		return this.money;
	}

	public int getDenomination() {
		return this.denomination;
	}

	public void setPinAccount(PinAccount pinAccount) {
		this.pinAccount = pinAccount;
	}

	public void removePinAccount() {
		this.pinAccount = null;
	}

	public PinAccount getPinAccount() {
		return this.pinAccount;
	}

}
