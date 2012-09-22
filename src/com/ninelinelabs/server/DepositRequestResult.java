package com.ninelinelabs.server;

public class DepositRequestResult {

	private boolean success;
	private long bank;
	private long credits;
	private long win;

	public DepositRequestResult(boolean success, long bank, long credits, long win) {
		this.bank = bank;
		this.credits = credits;
		this.win = win;
		this.success = success;
	}

	public long getBank() {
		return bank;
	}

	public long getCredits() {
		return credits;
	}

	public long getWin() {
		return win;
	}

	public boolean success() {
		return this.success;
	}
}
