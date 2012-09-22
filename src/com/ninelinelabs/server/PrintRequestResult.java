package com.ninelinelabs.server;

public class PrintRequestResult {
	private boolean success;
	private long timestamp;
	private long win;

	public PrintRequestResult(boolean success, long timestamp, long win) {
		this.success = success;
		this.timestamp = timestamp;
		this.win = win;
	}

	public boolean success() {
		return this.success;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public long getWin() {
		return this.win;
	}
}
