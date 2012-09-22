package com.ninelinelabs.server.processor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ninelinelabs.server.DepositRequestResult;
import com.ninelinelabs.server.TransactionProcessor;
import com.ninelinelabs.server.cashless.AccountNotFoundException;
import com.ninelinelabs.server.cashless.TransactionException;
import com.ninelinelabs.server.registry.AccountRegistry;
import com.ninelinelabs.server.registry.TerminalRegistry;
import com.ninelinelabs.util.database.ConnectionDispenser;

public class DepositRequestProcessor {

	public static final Logger logger = Logger.getLogger(DepositRequestProcessor.class.getName());

	public static DepositRequestResult processDepositRequest(String terminal, String game, String ticket, int playseq) {
		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			logger.log(Level.INFO, "Processing deposit request for terminal " + terminal + "; game : " + game);

			String ownBankAccnt = AccountRegistry.getAccountForTerminal(terminal);							// BANK account (player's own funds)
			String winBankAccnt = AccountRegistry.getWinAccountForTerminal(terminal);						// BANK account (player's win funds)

			String gameEscrowAccnt = AccountRegistry.getEscrowAccountForGameOnTerminal(terminal, game);

			String creditsGameAccnt = AccountRegistry.getCreditsAccountForGame(terminal, game);
			String creditsWinGameAccnt = AccountRegistry.getWinCreditsAccountForGame(terminal, game);

			int amount = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			if (amount < 0) {
				logger.log(Level.SEVERE, "ESCROW account balance is negative");
				throw new SQLException("ESCROW account balance is negative: " + amount);
			}

			logger.log(Level.INFO, terminal + " : Current balance for ESCROW account = " + amount);

			int operday = TerminalRegistry.getTerminalOperday(terminal);

			TransactionProcessor.recordTransaction(gameEscrowAccnt, creditsWinGameAccnt, amount, TransactionProcessor.WINDEPOSIT_TRANSACTION, "Card user", 10, "Player deposit transaction", operday);

			balance = TransactionProcessor.getAccountBalance(creditsGameAccnt, creditsWinGameAccnt);

			if (balance < 0) {
				logger.log(Level.SEVERE, "OWN&WIN CREDITS account balance is negative");
				throw new SQLException("OWN&WIN CREDITS account balance is negative: " + balance);
			}

			PreparedStatement ps = conn.prepareStatement("UPDATE PLAY_LOG SET TOTALWIN = ? WHERE TICKET = ? AND PLAYSEQ = ?");
			ps.setInt(1, amount);
			ps.setString(2, ticket);
			ps.setInt(3, playseq);

			ps.executeUpdate();

			long bankBalance = TransactionProcessor.getAccountBalance(ownBankAccnt, winBankAccnt);
			long creditsBalance = TransactionProcessor.getAccountBalance(creditsGameAccnt, creditsWinGameAccnt);

			conn.commit();

			return new DepositRequestResult(true, bankBalance, creditsBalance, 0L);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Database exception caught while processing deposit request",e);
			try {
				conn.rollback();
			} catch(SQLException sqle) {}
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Account not found exception caught while processing deposit request",e);
			try {
				conn.rollback();
			} catch(SQLException sqle) {}
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Transaction exception caught while processing deposit request",e);
			try {
				conn.rollback();
			} catch(SQLException sqle) {}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Generic exception caught while processing deposit request",e);
			try {
				conn.rollback();
			} catch(SQLException sqle) {}
		}
		finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new DepositRequestResult(false, 0L, 0L, 0L);
	}
}
