/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: AccountRegistry.java $
 *
 * Date Author Changes
 * Mar 30, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.cashless;

/**
 * A class containing factory methods for creating various types of accounts.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 * @see
 */
public class AccountRegistry {

	public TemporaryAccount getAccountForTerminal(String terminalId) {
		return new TemporaryAccount("TERMACCNT." + terminalId);
	}

	public TemporaryAccount getEscrowAccountForGameOnTerminal(String terminalId, String gameId) {
		return new TemporaryAccount("GAMEESCROW.TERM." + terminalId + ".GAME." + gameId);
	}

	public RollupAccount getLotteryProceedsAccountForGame(String gameId) {
		return new RollupAccount("LOTTO.PROCEEDS." + gameId);
	}

	public RollupAccount getLotteryFundAccountForGame(String gameId) {
		return new RollupAccount("LOTTO.FUND." + gameId);
	}

	public CashlessAccount getAccountForPinNo(String pin) {
		return new CashlessAccount("PIN." + pin);
	}

	public CashlessAccount getAccountForTerminalBNA(String terminal) {
		return new CashlessAccount("TERMINAL.BNA." + terminal);
	}

	public CashlessAccount getAccountForTerminalBNACash(String terminal) {
		return new CashlessAccount("TERMINAL.BNA.CASH." + terminal);
	}

	public RollupAccount getAccountForPinNoCash(String pin) {
		return new RollupAccount("PLAYER.CASH.PIN." + pin);
	}

	public TemporaryAccount getCreditsAccountForGame(String terminal, String game) {
		return new TemporaryAccount("CREDITS.GAME.TERM." + terminal + ".GAME." + game);
	}
}
