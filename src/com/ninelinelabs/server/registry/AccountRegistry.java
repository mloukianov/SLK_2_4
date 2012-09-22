package com.ninelinelabs.server.registry;

public class AccountRegistry {

	public static String getAccountForTerminal(String terminalId) {
		return "TERMACCNT." + terminalId;
	}

	public static String getWinAccountForTerminal(String terminalid) {
		return "TERMACCNT." + terminalid + ".WIN";
	}

	public static String getEscrowAccountForGameOnTerminal(String terminalId, String gameId) {
		return "GAMEESCROW.TERM." + terminalId + ".GAME." + gameId;
	}

	public static String getLotteryProceedsAccountForGame(String gameId) {
		return "LOTTO.PROCEEDS." + gameId;
	}

	public static String getLotteryFundAccountForGame(String gameId) {
		return "LOTTO.FUND." + gameId;
	}

	public static String getAccountForPinNo(String pin) {
		return "PIN." + pin;
	}

	public static String getWinAccountForPinNo(String pin) {
		return "PIN." + pin + ".WIN";
	}

	public static String getAccountForTerminalBNA(String terminal) {
		return "TERMINAL.BNA." + terminal;
	}

	public static String getAccountForTerminalBNACash(String terminal) {
		return "TERMINAL.BNA.CASH." + terminal;
	}

	public static String getAccountForPinNoCash(String pin) {
		return "PLAYER.CASH.PIN." + pin;
	}

	public static String getCreditsAccountForGame(String terminal, String game) {
		return "CREDITS.GAME.TERM." + terminal + ".GAME." + game;
	}

	public static String getWinCreditsAccountForGame(String terminal, String game) {
		return "CREDITS.GAME.TERM." + terminal + ".GAME." + game + ".WIN";
	}
}
