package com.ninelinelabs.server;

public class PlayRequestResult {
	private boolean enoughCreditsToPlay;
	private long bank;
	private long credits;
	private long win;

	public PlayRequestResult(boolean enoughCreditsToPlay, long bank, long credits, long win) {
		this.enoughCreditsToPlay = enoughCreditsToPlay;
		this.bank = bank;
		this.credits = credits;
		this.win = win;
	}

	public boolean hasEnoughCreditsToPlay() {
		return this.enoughCreditsToPlay;
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
