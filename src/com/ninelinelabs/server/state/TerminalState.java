package com.ninelinelabs.server.state;

public interface TerminalState {

	public static final int DEMO = 1;

	public static final int SERVICE = 2;

	public static final int LOBBY = 3;

	public static final int TICKET_RESERVED = 4;

	public static final int TICKET_PURCHASED = 5;

	public static final int SEG_REGULAR_PLAY = 6;

	public static final int SEG_REGULAR_ESCROW = 7;

	public static final int SEG_DOUBLE_PLAY = 8;

	public static final int SEG_DOUBLE_ESCROW = 9;

	public static final int SEG_BONUS_PLAY = 10;

	public static final int SEG_BONUS_ESCROW = 11;

	public static final int FREE_REGULAR_PLAY = 12;

	public static final int FREE_REGULAR_ESCROW = 13;

	public static final int COUNT = 14;

	public static final int INPLAY = 15;

	public static final int BLOCKED = 99;
	/*
	 *
	 * TerminalState from com.ninelinelabs.server
	 *
	public static final int UNAVAILABLE = 1;
	public static final int DEMO = 2;
	public static final int INPLAY = 3;
	public static final int SERVICE = 4;
	public static final int COUNT = 5;
	public static final int BLOCKED = 99;
	 */
}
