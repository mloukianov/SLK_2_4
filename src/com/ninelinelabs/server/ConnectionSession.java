/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ConnectionSession.java 65 2011-05-20 01:27:40Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 25, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

import java.io.Serializable;
import java.util.HashMap;

import com.ninelinelabs.server.cashless.Account;
import com.ninelinelabs.server.cashless.RollupAccount;
import com.ninelinelabs.server.cashless.TemporaryAccount;

/**
 * Session class for terminal connection. Serializable, so we should
 * be able to use it in session replication when needed.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 65 $ $Date: 2011-05-19 20:27:40 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class ConnectionSession implements Serializable {

	private static final long serialVersionUID = 1L;

	private Account bank;
	private Account credit;
	private Account bet;
	private Account win;
	private Account escrow;

	public static final String[] state_names = new String[] {
													"DISCONNECTED_STATE",
													"CONNECTED_STATE",
													"LOBBY_STATE",
													"GAME_STATE",
													"WIN_STATE",
													"DOUBLEUP_STATE",
													"TECH_STATE",
													"SENTINEL_STATE"};
	public enum State {
		DISCONNECTED_STATE(0), CONNECTED_STATE(1), LOBBY_STATE(2), GAME_STATE(3),
		WIN_STATE(4), DOUBLEUP_STATE(5), TECH_STATE(6), SENTINEL_STATE(7);

		private int i;

		State(int i) {
			this.i = i;
		}

		public int num() {
			return i;
		}
	}

	private HashMap<String, String> properties = new HashMap<String, String>();

	static final String GAME = "game";

	static final String TERMINAL = "terminal";

	static final String PIN = "pin";

	static final String LASTPIN = "lastpin";


	public ConnectionSession() {}

	public void put(String key, String value) {
		properties.put(key, value);
	}

	public String get(String key) {
		return properties.get(key);
	}

	public void remove(String key) {
		this.properties.remove(key);
	}

	public void removeAll() {
		this.properties.clear();
	}

	public void createAccountBank(String accountno) {
		this.bank = new TemporaryAccount(accountno);
	}

	public Account getAccountBank() {
		return this.bank;
	}

	public void createAccountCredit(String accountno) {
		this.credit = new TemporaryAccount(accountno);
	}

	public Account getAccountCredit() {
		return this.credit;
	}

	public void createAccountBet(String accountno) {
		this.bet = new RollupAccount(accountno);
	}

	public Account getAccountBet() {
		return this.bet;
	}

	public void createAccountWin(String accountno) {
		this.win = new RollupAccount(accountno);
	}

	public Account getAccountWin() {
		return this.win;
	}

	public void createAccountEscrow(String accountno) {
		this.escrow = new TemporaryAccount(accountno);
	}

	public Account getAccountEscrow() {
		return this.escrow;
	}
}
