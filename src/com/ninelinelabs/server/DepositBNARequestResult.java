package com.ninelinelabs.server;

public class DepositBNARequestResult {
	private boolean success;
	private long bank;
	private long credits;
	private long win;

	public DepositBNARequestResult(boolean success, long bank, long credits, long win) {
		this.success = success;
		this.bank = bank;
		this.credits = credits;
		this.win = win;
	}

	public boolean success() {
		return this.success;
	}

	public long getBank() {
		return this.bank;
	}

	public long getCredits() {
		return this.credits;
	}

	public long getWin() {
		return this.win;
	}
}
