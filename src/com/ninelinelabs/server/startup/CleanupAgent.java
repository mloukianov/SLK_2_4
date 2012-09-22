/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: CleanupAgent.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * Mar 09, 2010 mloukianov Changed connection management mechanism to use ThreadLocal
 *
 */
package com.ninelinelabs.server.startup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;

import com.ninelinelabs.server.Denomination;
import com.ninelinelabs.server.RequestProcessor;
import com.ninelinelabs.server.TransactionProcessor;
import com.ninelinelabs.server.cashless.AccountNotFoundException;
import com.ninelinelabs.server.cashless.TransactionException;
import com.ninelinelabs.util.database.ConnectionDispenser;

/**
 * Performs cleanup for stale accounts at server startup.
 * 
 * @author mloukianov
 */
public class CleanupAgent {

	private static final Logger logger = Logger.getLogger(CleanupAgent.class.getName());

	
	static {
		try {
			final StandardServer server = (StandardServer)ServerFactory.getServer();
			final Context ctx = server.getGlobalNamingContext();

			if (ctx != null) {
				DataSource ds = (DataSource)ctx.lookup("jdbc/GameServerDB");
				ConnectionDispenser.setDataSource(ds);
			} else {
				logger.log(Level.SEVERE, "DataSource is null");
			}
		} catch (NamingException ne) {
			logger.log(Level.SEVERE, "Exception getting DataSource from JNDI", ne);
		}
	}
	
	
	public CleanupAgent() {
		
	}
	
	public void cleanup() throws SQLException {
		Connection conn = null;
		
		try {
			conn = ConnectionDispenser.getNewConnection(false);
				
				// perform cleanup for every terminal
				
				PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID, HALLNAME, PINACCOUNT, WINACCOUNT, STATE, CARDNO, GAME FROM TERMINAL WHERE CARDNO IS NOT NULL");
				
				ResultSet rs = ps.executeQuery();
				
				while (rs.next()) {

					@SuppressWarnings("unused")
					int terminalId = rs.getInt(1);
					String terminal = rs.getString(2);
					String pinAccount = rs.getString(3);
					String winAccount = rs.getString(4);
					@SuppressWarnings("unused")
					String state = rs.getString(5);
					String cardno = rs.getString(6);
					String game = rs.getString(7);
					
					int operday = RequestProcessor.getTerminalOperday(terminal);

					if (game != null) {
						// move money from game escrow account to game account
						
						String gameEscrowAccount = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);
						String gameWinAccount = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
						String gameAccount = RequestProcessor.getCreditsAccountForGame(terminal, game);
						
						Denomination denom = RequestProcessor.getDenomination(terminal);
						
						int gameEscrowBalance = TransactionProcessor.getAccountBalance(gameEscrowAccount);
						
						if (gameEscrowBalance > 0) {
							TransactionProcessor.recordTransaction(gameEscrowAccount, gameWinAccount, gameEscrowBalance, TransactionProcessor.WINDEPOSIT_TRANSACTION, "cleanup agent", 1, "Escrow account cleanup", operday);
						}
						
						int gameWinBalance = TransactionProcessor.getAccountBalance(gameWinAccount);
						
						if (gameWinBalance > 0) {
							TransactionProcessor.recordBackConversion(gameWinAccount, denom.getCreditsAccount(), denom.getCurrencyAccount(), winAccount, gameWinBalance, denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "cleanup agent", 1, "Game account cleanup", operday);
						}
						
						int gameOwnBalance = TransactionProcessor.getAccountBalance(gameAccount);
						
						if (gameOwnBalance > 0) {
							TransactionProcessor.recordBackConversion(gameAccount, denom.getCreditsAccount(), denom.getCurrencyAccount(), pinAccount, gameOwnBalance, denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "cleanup agent", 1, "Game account cleanup", operday);
						}
						
					}
					
					int terminalWinBalance = TransactionProcessor.getAccountBalance(winAccount);
					
					String cardWinAccount = RequestProcessor.getWinAccountForPinNo(cardno); 
					
					if (terminalWinBalance > 0) {
						TransactionProcessor.recordTransaction(winAccount, cardWinAccount, terminalWinBalance, TransactionProcessor.CASHOUT_TRANSACTION, "cleanup agent", 1, "Terminal account cleanup", operday);
					}
					
					int terminalOwnBalance = TransactionProcessor.getAccountBalance(pinAccount); 
						
					String cardOwnAccount =	RequestProcessor.getAccountForPinNo(cardno);
					
					if (terminalOwnBalance > 0) {
						TransactionProcessor.recordTransaction(pinAccount, cardOwnAccount, terminalOwnBalance, TransactionProcessor.CASHOUT_TRANSACTION, "cleanup agent", 1, "Terminal account cleanup", operday);
					}
					
					RequestProcessor.setTerminalState(terminal, "UNAVAILABLE");
				}

				ps = conn.prepareStatement("UPDATE TERMINAL SET STATE='UNAVAILABLE'");
				
				ps.executeUpdate();
				
				ps = conn.prepareStatement("UPDATE SESSION_LOG SET END=NOW(), STATUS='CLOSED' WHERE STATUS='ACTIVE'");
				
				int res = ps.executeUpdate();
				
				logger.log(Level.INFO, "Cleanup agent found " + res + " terminals with hanging sessions that were closed up");
				
				conn.commit();
				
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Can not perform accounts cleanup", e);
		} catch (AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Can not find account", e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Transaction exception", e);
		} finally {
			conn.close();
			ConnectionDispenser.releaseConnection();
		}
	}
}
