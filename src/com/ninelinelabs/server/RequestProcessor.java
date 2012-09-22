/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: RequestProcessor.java 282 2011-10-18 13:57:20Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Jan 19, 2009 mloukianov Fixed issues with System.out.println()
 * Jan 29, 2009 mloukianov General cleanup
 * Mar 14, 2009 mloukianov Major changes in transaction processing and logging
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * Mar 07, 2010 mloukianov Changes to use ThreadLocal to manage database connection in thread
 * Mar 09, 2010 mloukianov Fixed problems with top-level database connection closing
 * Mar 31, 2010 mloukianov Added commands to support paper tickets for Ukraine project (ROLL_*)
 * Jun 27, 2010 mloukianov Added support for moving rolls between terminals
 * Aug 28, 2011 mloukianov Changed processPlayLongTicketRequest return type to boolean to indicate failure (e.g. in case there was not enough credits to continue playing within that ticket)
 * Sep 05, 2011 mloukianov Added inserting records into UTILIZED_TICKETS_STATS table to utilizePaperTickets() methods to create the utilized tickets data for stats
 * Oct 18, 2011 mloukianov Fixed processDepositBNARequest() method (case when no game is selected)
 * Oct 18, 2011 mloukianov Fixed connectTerminal() method - added missing string parameter to sql query
 */
package com.ninelinelabs.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;

import com.ninelinelabs.authentication.vo.AuthenticationResult;
import com.ninelinelabs.game.slots.vo.GameStops;
import com.ninelinelabs.game.slots.vo.LotteryGameResult;
import com.ninelinelabs.lottery.generator.vo.LongTicketVO;
import com.ninelinelabs.lottery.generator.vo.bor.BorLongTicketVO;
import com.ninelinelabs.server.cashless.AccountNotFoundException;
import com.ninelinelabs.server.cashless.TransactionException;
import com.ninelinelabs.server.processor.DepositRequestProcessor;
import com.ninelinelabs.util.database.ConnectionDispenser;

/**
 * Class for processing game requests.
 * The class is stateless; all requests are processed using static functions
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 282 $ $Date: 2011-10-18 08:57:20 -0500 (Tue, 18 Oct 2011) $
 * @see
 */
public class RequestProcessor {

	public static final int SECURITY_EVENT_LOGIN_ATTEMP = 1;
	public static final int SECURITY_EVENT_SUCCESFUL_LOGIN = 2;
	public static final int SECURITY_EVENT_FAILED_LOGIN = 3;

	public static final String DATASOURCE_JNDI_NAME = "jdbc/GameServerDB";

	static final Logger logger = Logger.getLogger(RequestProcessor.class.getName());


	/*
	 * get data source and pass it to ThreadLocal for later use
	 */
	static {

		try {

			final StandardServer server = (StandardServer)ServerFactory.getServer();
			final Context ctx = server.getGlobalNamingContext();

			if (ctx != null) {
				DataSource ds = (DataSource)ctx.lookup("jdbc/GameServerDB");
				ConnectionDispenser.setDataSource(ds);
			} else {
				logger.log(Level.SEVERE, "Can not retrieve DataSource for " + RequestProcessor.DATASOURCE_JNDI_NAME);
				throw new RuntimeException("Can not retrieve DataSource for " + RequestProcessor.DATASOURCE_JNDI_NAME);
			}

		} catch (NamingException ne) {
			logger.log(Level.SEVERE, "Can not get DataSource; NamingException caught", ne);
		}
	}

	/**
	 * Processes BNA money deposit.
	 *
	 * Note for split win accounting:
	 * - the amount deposited in terminal's BNA is placed in regular terminal account
	 * - transaction is performed between terminal BNA's non-cash account and terminal account for this session
	 * - cash transaction is performed between player cash settlement account and terminal BNA's cash account
	 *
	 * @param terminal   terminal identifier
	 * @param amount     amount of money deposited
	 * @param pin        PIN number
	 *
	 * @return  terminal account balance
	 */
	public static DepositBNARequestResult processDepositBNARequest(String terminal, String game, int amount, String pin) throws WrongBanknoteException {

		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			if (!RequestProcessor.checkBanknote(amount)) {
				logger.log(Level.SEVERE, "Wrong banknote denomination received from terminal: " + amount);
				throw new WrongBanknoteException(amount);
			}

			RequestProcessor.increaseBanknoteCount(terminal, amount);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			String creditsOwnAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String creditsWinAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			String escrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);

			String playerCashAccnt = RequestProcessor.getAccountForPinNoCash(pin);

			String terminalBNACashAccnt = RequestProcessor.getAccountForTerminalBNACash(terminal);
			String terminalBNAAccnt = RequestProcessor.getAccountForTerminalBNA(terminal);

			int operday = getTerminalOperday(terminal);

			TransactionProcessor.recordCashTransaction(playerCashAccnt, terminalBNACashAccnt, terminalBNAAccnt, terminalAccnt, amount,
												TransactionProcessor.TERMINALBNACASHLOAD_TRANSACTION, "Card user", 10, "BNA Cash Deposit", operday);

			balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);

			PreparedStatement ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
			ps.setString(1, pin);

			ResultSet rs = ps.executeQuery();

			int cardid = 0;

			if (rs.next()) {
				cardid = rs.getInt(1);
			}

			ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, NULL, ?, ?, ?, ?, ?)");
			ps.setInt(1, cardid);
			ps.setInt(2, 3);		// 3 - financial transaction
			ps.setInt(3, 93);		// 93 - money deposited to the card at the terminal
			ps.setInt(4, 0);		// debit - 0
			ps.setInt(5, amount);	// credit - deposit amount
			ps.setLong(6, balance);	// balance - new terminal balance
			ps.setInt(7, operday);	// current operday for that terminal
			ps.setInt(8, 0);		// register number not available

			ps.executeUpdate();

			long bankBalance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);

			long creditsBalance = 0L;
			long winBalance = 0L;

			if (game != null && game.length() != 0) {
				creditsBalance = TransactionProcessor.getAccountBalance(creditsOwnAccnt, creditsWinAccnt);
				winBalance = TransactionProcessor.getAccountBalance(escrowAccnt);
			}

			conn.commit();

			return new DepositBNARequestResult(true, bankBalance, creditsBalance, winBalance);

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.log(Level.SEVERE, "Can not roll back transaction", e1);
			}
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.log(Level.SEVERE, "Can not roll back transaction", e1);
			}
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.log(Level.SEVERE, "Can not roll back transaction", e1);
			}
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new DepositBNARequestResult(false, 0L, 0L, 0L);
	}


	private static boolean checkBanknote(int amount) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT BANKNOTE FROM BANKNOTE WHERE BANKNOTE = ?");

		ps.setInt(1, amount);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			return true;
		} else {
			return false;
		}
	}


	public static int getTerminalOperday(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT OPERDAY FROM TERMINAL WHERE HALLNAME= ?");

		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int operday = rs.getInt(1);
			return operday;
		}

		return -1;
	}


	private static int getCashRegisterOperday() throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT MAX(OPERDAY) FROM CASHIER_SESSION");

		ResultSet rs = ps.executeQuery();

		int operday = 0;

		if (rs.next()) {
			operday = rs.getInt(1);
		}

		return operday;
	}


	@SuppressWarnings("unused")
	private static int getCashRegisterOperday(int register) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT OPERDAY FROM CASH_REGISTER WHERE REGISTERID = ?");

		ResultSet rs = ps.executeQuery();

		int operday = 0;

		if (rs.next()) {
			operday = rs.getInt(1);
		}

		return operday;
	}



	private static void increaseBanknoteCount(String terminal, int amount) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID, OPERDAY FROM TERMINAL WHERE HALLNAME= ?");
		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		int terminalid = 0;
		int operday = 0;

		if (rs.next()) {
			terminalid = rs.getInt(1);
			operday = rs.getInt(2);
		} else {
			logger.log(Level.SEVERE, "Can not find terminal informaton for terminal {0}", terminal);
			throw new SQLException("Can not find terminal information for terminal " + terminal);
		}

		logger.log(Level.INFO, "Increasing banknote count for terminal " + terminal + " with banknote " + amount + " in operday " + operday);

		PreparedStatement incrementBanknoteCount = conn.prepareStatement("INSERT INTO COUNT_ENTRY (COUNT, TERMINAL, BANKNOTE, BANKNOTE_COUNT) VALUES (0, ?, ?, 1) ON DUPLICATE KEY UPDATE BANKNOTE_COUNT = BANKNOTE_COUNT + 1");

		incrementBanknoteCount.setInt(1, terminalid);
		incrementBanknoteCount.setInt(2, amount);

		if (incrementBanknoteCount.executeUpdate() < 1) {
			logger.log(Level.SEVERE, "Can not increment banknote count; rows affected < 1");
			throw new SQLException("Can not increment banknote count");
		}
	}


	@Deprecated
	public static void processConnectionLost(String terminal, String pin, BorLongTicketVO bonusticket, String game) {

		Connection conn = null;

		logger.log(Level.INFO, "Processing connection lost for terminal " + terminal + "; pin = " + pin + "; game = " + game);

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			if (game != null) {	// we are in the game; need to close the ticket and credit win and remaining credits to the card

				String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);
				String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
				String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
				String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
				String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

				int escrowBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

				if (escrowBalance < 0) {
					logger.log(Level.SEVERE, "ESCROW account balance is negative");
					throw new SQLException("ESCROW account balance is negative: " + escrowBalance);
				}

				int operday = RequestProcessor.getTerminalOperday(terminal);

				TransactionProcessor.recordTransaction(gameEscrowAccnt, gameWinCreditsAccnt, escrowBalance, TransactionProcessor.WINDEPOSIT_TRANSACTION, "conection lost: PIN: " + pin, 10, "Connection lost funds recovery", operday);

				int creditsBalance = TransactionProcessor.getAccountBalance(gameCreditsAccnt);
				if (creditsBalance < 0) {
					logger.log(Level.SEVERE, "CREDITS account balance is negative");
					throw new SQLException("CREDITS account balance is negative: " + creditsBalance);
				}

				int creditsWinBalance = TransactionProcessor.getAccountBalance(gameWinCreditsAccnt);
				if (creditsWinBalance < 0) {
					logger.log(Level.SEVERE, "CREDITS WIN account balance is negative");
					throw new SQLException("CREDITS WIN account balance is negative: " + creditsWinBalance);
				}

				Denomination denom = RequestProcessor.getDenomination(terminal);

				if (creditsBalance > 0)
					TransactionProcessor.recordBackConversion(gameCreditsAccnt, denom.getCreditsAccount(), denom.getCurrencyAccount(), terminalAccnt, creditsBalance,
												denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "Card user", 10, "Player exits lottery", operday);

				if (creditsWinBalance > 0)
					TransactionProcessor.recordBackConversion(gameWinCreditsAccnt, denom.getCreditsAccount(), denom.getCurrencyAccount(), terminalWinAccnt, creditsWinBalance,
												denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "Card user", 10, "Player exits lottery", operday);


				TransactionProcessor.recordTransaction(gameCreditsAccnt, terminalAccnt, creditsBalance, TransactionProcessor.CREDITSTOBANK_TRANSACTION, "connection lost: PIN: " + pin, 10, "Connection lost funds recovery", operday);
				TransactionProcessor.recordTransaction(gameWinCreditsAccnt, terminalWinAccnt, creditsWinBalance, TransactionProcessor.CREDITSTOBANK_TRANSACTION, "connection lost: PIN: " + pin, 10, "Connection lost funds recovery", operday);

				// RequestProcessor.addLotteryLogRecord(conn, userid, ticketno, amount, bet, cellno, win, operday, recordtype)
			}

			if (pin != null) {	// there was a card in a terminal; need to credit both own and win funds back to the card

				String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
				String pinAccnt = RequestProcessor.getAccountForPinNo(pin);
				String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);
				String pinWinAccnt = RequestProcessor.getWinAccountForPinNo(pin);

				int terminalBalance = TransactionProcessor.getAccountBalance(terminalAccnt);
				if (terminalBalance < 0) {
					logger.log(Level.SEVERE, "TERMINAL account balance is negative");
					throw new SQLException("TERMINAL account balance is negative: " + terminalBalance);
				}

				int terminalWinBalance = TransactionProcessor.getAccountBalance(terminalWinAccnt);
				if (terminalWinBalance < 0) {
					logger.log(Level.SEVERE, "TERMINAL WIN account balance is negative");
					throw new SQLException("TERMINAL WIN account balance is negative: " + terminalWinBalance);
				}

				int operday = RequestProcessor.getTerminalOperday(terminal);

				TransactionProcessor.recordTransaction(terminalAccnt, pinAccnt, terminalBalance, TransactionProcessor.CASHOUT_TRANSACTION, "connection lost: PIN: " + pin, 10, "Connection lost funds recovery", operday);
				TransactionProcessor.recordTransaction(terminalWinAccnt, pinWinAccnt, terminalWinBalance, TransactionProcessor.CASHOUT_TRANSACTION, "connection lost: PIN: " + pin, 10, "Connection lost funds recovery", operday);
			}

			RequestProcessor.setTerminalState(terminal, Terminal.UNAVAILABLE_STATE);

			conn.commit();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing abnormal connection termination", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}
	}


	public static void addSessionLogRecord(String terminal, String pin, String sessionId) {

	}


	public static int processExitLotteryRequest(String terminal, String game, String cardno, String ticketno, boolean returned) {

		Connection conn = null;
		int balance = 0;

		if (ticketno == null || ticketno.equals("")) {
			logger.log(Level.SEVERE, ">>>> No ticket number provided when exiting lottery game");
		}

		try {
			conn = ConnectionDispenser.getNewConnection(false);

				String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
				String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
				String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
				String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

				int amount = TransactionProcessor.getAccountBalance(gameCreditsAccnt);
				if (amount < 0) {
					logger.log(Level.SEVERE, "OWN CREDITS account balance is negative");
					throw new SQLException("OWN CREDITS account balance is negative: " + amount);
				}

				int winAmount = TransactionProcessor.getAccountBalance(gameWinCreditsAccnt);
				if (winAmount < 0) {
					logger.log(Level.SEVERE, "WIN CREDITS account balance is negative");
					throw new SQLException("WIN CREDITS account balance is negative: " + winAmount);
				}

				int operday = RequestProcessor.getTerminalOperday(terminal);
				int terminalid = RequestProcessor.getTerminalId(terminal);
				Denomination denom = RequestProcessor.getDenomination(terminal);

				RequestProcessor.setTerminalGame(terminal, null);

				if (amount > 0)
                    TransactionProcessor.recordBackConversion(gameCreditsAccnt, denom.getCreditsAccount(), denom.getCurrencyAccount(), terminalAccnt, amount,
												denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "Card user", 10, "Player exits lottery", operday);

				if (winAmount > 0)
                    TransactionProcessor.recordBackConversion(gameWinCreditsAccnt, denom.getCreditsAccount(), denom.getCurrencyAccount(), terminalWinAccnt, winAmount,
												denom.getExchangeRate(), TransactionProcessor.CREDITSTOBANK_TRANSACTION, "Card user", 10, "Player exits lottery", operday);

				// note - we return total balance for two accounts!
				balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);

				if (balance < 0) {
					logger.log(Level.SEVERE, "Total BANK balance is negative: {0}", balance);
					throw new SQLException("Total BANK balance is negative: " + balance);
				}

				int userid = RequestProcessor.getUseridForCardno(cardno);

				RequestProcessor.addLotteryLogRecord(userid, ticketno, (amount + winAmount), 0, 0, 0, operday, RequestProcessor.LLOG_TICKET_PAYOUT, terminalid);

				PreparedStatement ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
				ps.setString(1, cardno);

				ResultSet rs = ps.executeQuery();

				int cardid = 0;

				if (rs.next()) {
					cardid = rs.getInt(1);
				}

				if (!returned) {

					ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					ps.setInt(1, cardid);
					ps.setInt(2, 1);
					ps.setInt(3, 5);
					ps.setString(4, ticketno);
					ps.setInt(5, 0);
					ps.setInt(6, (amount + winAmount) * denom.getExchangeRate());
					ps.setLong(7, balance);
					ps.setInt(8, operday);
					ps.setInt(9, 0);

					ps.executeUpdate();

					ps = conn.prepareStatement("UPDATE TICKET_LOG SET CASHEDOUT = NOW(), TOTALWIN = ?, UTILIZED = ? WHERE TICKET = ?");
					ps.setInt(1, (amount + winAmount) * denom.getExchangeRate());
					ps.setString(3, ticketno);
					ps.setBoolean(2, returned);

					ps.executeUpdate();

				} else {

					ps = conn.prepareStatement("SELECT BABINA FROM PAPERTICKET WHERE TICKET = ?");
					ps.setString(1, ticketno);

					rs = ps.executeQuery();

					if (rs.next()) {

						String babina = rs.getString(1);

						ps = conn.prepareStatement("UPDATE BB_TRACKER SET NEXT_TICKET = NEXT_TICKET - 1 WHERE BARCODE = ?");
						ps.setString(1, babina);

						ps.executeUpdate();
					}

					logger.log(Level.INFO, "Processing returned ticket no " + ticketno + ":");

					ps = conn.prepareStatement("UPDATE LONGTICKET SET STATUS = 1 WHERE TICKET = ?");
					ps.setString(1, ticketno);

					int res = ps.executeUpdate();

					logger.log(Level.INFO, "Updated " + res + " rows in LONGTICKET table");

					ps = conn.prepareStatement("DELETE FROM LOTTERY_LOG WHERE TICKETNO = ?");
					ps.setString(1, ticketno);

					res = ps.executeUpdate();

					logger.log(Level.INFO, "Deleted " + res + " rows from LOTTERY_LOG table");

					ps = conn.prepareStatement("DELETE FROM PAPERTICKET WHERE TICKET = ?");
					ps.setString(1, ticketno);

					res = ps.executeUpdate();

					logger.log(Level.INFO, "Deleted " + res + " rows from PAPERTICKET table");

					ps = conn.prepareStatement("DELETE FROM PLAYERCARD_LOG WHERE TICKET = ?");
					ps.setString(1, ticketno);

					res = ps.executeUpdate();

					logger.log(Level.INFO, "Deleted " + res + " rows from PLAYERCARD_LOG table");

					ps = conn.prepareStatement("DELETE FROM TICKET_LOG WHERE TICKET = ?");
					ps.setString(1, ticketno);

					res = ps.executeUpdate();

					logger.log(Level.INFO, "Deleted " + res + " rows from TICKET_LOG table");

				}

				conn.commit();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
			try {
				conn.rollback();
			}
			catch (SQLException sqle) {}
		} finally {
			try {
				conn.close();
			}
			catch (SQLException e) {}

			ConnectionDispenser.releaseConnection();
		}

		return balance;
	}

	public static boolean processRollTktRequest(String terminal, String rollid) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			logger.log(Level.FINE, "Reservation made for a ticket in roll " + rollid);

			return true;

			/*
			PreparedStatement ps = conn.prepareStatement("UPDATE BABINA SET BARCODE = NULL WHERE TERMINALID = ? AND BARCODE = ?");
			ps.setString(2, rollid);
			ps.setString(1, terminal);

			int res = ps.executeUpdate();

			if (res < 1) {
				return false;
			} else {
				return true;
			}
			*/
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception while doing roll init", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch(Exception e) {}
		}

		return false;
	}

	public static boolean processRollErr(String terminal, String rollid) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			/*
			PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID FROM TERMINAL WHERE HALLNAME = ?");
			ps.setString(1, terminal);

			ResultSet rs = ps.executeQuery();

			int terminalid = 0;
			if (rs.next()) {
				terminalid = rs.getInt(1);
			}

			ps = conn.prepareStatement("DELETE FROM BABINA WHERE TERMINALID = ? AND BARCODE = ?");
			ps.setString(2, rollid);
			ps.setInt(1, terminalid);

			int res = ps.executeUpdate();

			conn.commit();

			if (res < 1) {
				return false;
			} else {
				return true;
			}
			*/

			return true;

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception while doing roll init", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch(Exception e) {}
		}

		return false;
	}

	public static boolean processRollEnd(String terminal, String rollid) {

		/*
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID, OPERDAY FROM TERMINAL WHERE HALLNAME = ?");
			ps.setString(1, terminal);

			ResultSet rs = ps.executeQuery();

			int terminalid = 0;

			@SuppressWarnings("unused")
			int operday = 0;

			if (rs.next()) {
				terminalid = rs.getInt(1);
				operday = rs.getInt(2);
			}

			ps = conn.prepareStatement("DELETE FROM BABINA WHERE TERMINALID = ? AND BARCODE = ?");
			ps.setString(2, rollid);
			ps.setInt(1, terminalid);

			ps.executeUpdate();

			conn.commit();

			return true;

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception while doing roll init", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch(Exception e) {}
		}

		return false;
		*/

		return true;
	}

	private static PaperTicketRoll getRoll(String barcode, Connection conn) throws SQLException {

		PaperTicketRoll roll = null;

		PreparedStatement ps = conn.prepareStatement("SELECT INSTALLED, TERMINALID FROM BABINA WHERE BARCODE = ?");
		ps.setString(1, barcode);

		ResultSet rs = ps.executeQuery();

		int terminalid = 0;
		boolean initialized = false;

		if (rs.next()) {
			initialized = rs.getBoolean(1);
			terminalid = rs.getInt(2);

			roll = new PaperTicketRoll();
			roll.setBarcode(barcode);
			roll.setTerminalid(terminalid);
			roll.setInitialized(initialized);

			ps = conn.prepareStatement("SELECT NEXT_TICKET, TERMINAL FROM BB_TRACKER WHERE BARCODE = ?");
			ps.setString(1, barcode);

			rs = ps.executeQuery();

			if (rs.next()) {
				int next_ticket = rs.getInt(1);
				int terminal = rs.getInt(2);
				roll.setNextTicket(next_ticket);

				if (terminal != terminalid) {
					logger.log(Level.SEVERE, "BB_TRACKER terminal id mismatch for barcode " + barcode + "; BABINA terminal id = " + terminalid + " != BB_TRACKER terminal id = " + terminal);
				}

				logger.log(Level.INFO, "Roll initialization: terminalid=" + terminal + " for barcode=" + barcode + "; next_ticket=" + next_ticket);

			} else {
				ps = conn.prepareStatement("INSERT INTO BB_TRACKER(TERMINAL, BARCODE, NEXT_TICKET) VALUES(?, ?, ?)");
				ps.setInt(1, terminalid);
				ps.setString(2, barcode);
				ps.setInt(3, RequestProcessor.FIRST_BB_TICKET_NO);

				ps.executeUpdate();

				roll.setNextTicket(RequestProcessor.FIRST_BB_TICKET_NO);

				logger.log(Level.INFO, "Roll initialization: terminalid=" + terminalid + " for barcode=" + barcode + "; next_ticket=" + RequestProcessor.FIRST_BB_TICKET_NO);
			}
		}

		return roll;
	}


	private static TerminalInfo getTerminalInfo(String name, Connection conn) throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID, OPERDAY FROM TERMINAL WHERE HALLNAME = ?");
		ps.setString(1, name);

		ResultSet rs = ps.executeQuery();

		int terminalid = 0;
		int operday = 0;

		if (rs.next()) {
			terminalid = rs.getInt(1);
			operday = rs.getInt(2);

			TerminalInfo terminal = new TerminalInfo();
			terminal.setName(name);
			terminal.setTerminalid(terminalid);
			terminal.setOperday(operday);

			return terminal;
		} else {
			return null;
		}
	}


	private static PaperTicketRoll installRoll(String barcode, int toTerminal, Connection conn) throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT BARCODE FROM BABINA WHERE TERMINALID = ?");
		ps.setInt(1, toTerminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {

			String prevbarcode = rs.getString(1);
			logger.log(Level.INFO, "Found previous roll (barcode " + prevbarcode + ") installed at terminal " + toTerminal);

			ps = conn.prepareStatement("UPDATE BABINA SET INSTALLED = FALSE, TERMINALID = NULL WHERE BARCODE = ?");
			ps.setString(1, prevbarcode);

			ps.executeUpdate();

			ps = conn.prepareStatement("UPDATE BB_TRACKER SET TERMINAL = 0 WHERE BARCODE = ?");
			ps.setString(1, prevbarcode);

			ps.executeUpdate();
		}

		// assign terminal to this roll

		logger.log(Level.INFO, "Installing new roll " + barcode + " on terminal id = " + toTerminal);

		ps = conn.prepareStatement("UPDATE BABINA SET TERMINALID = ?, INSTALLED = TRUE WHERE BARCODE = ?");
		ps.setInt(1, toTerminal);
		ps.setString(2, barcode);

		ps.executeUpdate();

		int next_ticket = FIRST_BB_TICKET_NO;

		ps = conn.prepareStatement("INSERT INTO BB_TRACKER(TERMINAL, BARCODE, NEXT_TICKET) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE BB_TRACKER.TERMINAL = ?");
		ps.setInt(1, toTerminal);
		ps.setString(2, barcode);
		ps.setInt(3, next_ticket);
		ps.setInt(4, toTerminal);

		ps.executeUpdate();

		ps = conn.prepareStatement("SELECT NEXT_TICKET FROM BB_TRACKER WHERE BARCODE =? AND TERMINAL = ?");
		ps.setString(1, barcode);
		ps.setInt(2, toTerminal);

		rs = ps.executeQuery();

		if (rs.next()) {
			next_ticket = rs.getInt(1);
			logger.log(Level.INFO, "Next expected ticket for barcode " + barcode + " is " + next_ticket);
		} else {
			logger.log(Level.SEVERE, "Next expected ticket number is not found in BB_TRACKER; setting next expected ticket to FIRST_BB_TICKET_NO");
		}

		// create roll information

		PaperTicketRoll roll = new PaperTicketRoll();
		roll.setBarcode(barcode);
		roll.setInitialized(true);
		roll.setTerminalid(toTerminal);
		roll.setNextTicket(next_ticket);

		return roll;
	}


	private static PaperTicketRoll moveRoll(String barcode, int fromTerminal, int toTerminal, Connection conn) throws SQLException {

		// get barcode from the terminal roll is being installed on
		// (to check if that terminal has another roll already installed)
		PreparedStatement ps = conn.prepareStatement("SELECT BARCODE FROM BABINA WHERE TERMINALID = ?");
		ps.setInt(1, toTerminal);

		ResultSet rs = ps.executeQuery();

		String prevbarcode = "";

		if (rs.next()) {
			// the terminal already has roll installed
			prevbarcode = rs.getString(1);
			logger.log(Level.INFO, "Roll initialization: found another roll installed on this terminal; barcode=" + prevbarcode);
		}

		ps = conn.prepareStatement("UPDATE BB_TRACKER SET TERMINAL = NULL WHERE BARCODE = ?");
		ps.setString(1, prevbarcode);

		int res = ps.executeUpdate();

		if (res > 0) {
			logger.log(Level.INFO, "Roll initialization: removed terminal info from BB_TRACKER for barcode=" + barcode);
		}

		ps = conn.prepareStatement("UPDATE BABINA SET INSTALLED = FALSE, TERMINALID = NULL WHERE TERMINALID = ?");
		ps.setInt(1, fromTerminal);

		res = ps.executeUpdate();

		if (res > 0) {
			logger.log(Level.INFO, "Roll initialization: marked roll as not installed for terminalid=" + fromTerminal);
		}

		ps = conn.prepareStatement("UPDATE BABINA SET INSTALLED = FALSE, TERMINALID = NULL WHERE TERMINALID = ?");
		ps.setInt(1, toTerminal);

		ps.executeUpdate();

		// mark roll as installed at new terminal; this will automatically move it to new terminal

		ps = conn.prepareStatement("UPDATE BABINA SET TERMINALID = ?, INSTALLED = TRUE WHERE BARCODE = ?");
		ps.setInt(1, toTerminal);
		ps.setString(2, barcode);

		ps.executeUpdate();

		int next_ticket = FIRST_BB_TICKET_NO;

		// create next expected ticket information for "to" terminal
		ps = conn.prepareStatement("INSERT INTO BB_TRACKER(TERMINAL, BARCODE, NEXT_TICKET) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE BB_TRACKER.BARCODE = BB_TRACKER.BARCODE, BB_TRACKER.TERMINAL = ?");
		ps.setInt(1, toTerminal);
		ps.setString(2, barcode);
		ps.setInt(3, next_ticket);
		ps.setInt(4, toTerminal);

		ps.executeUpdate();

		ps = conn.prepareStatement("SELECT NEXT_TICKET FROM BB_TRACKER WHERE BARCODE = ? AND TERMINAL = ?");
		ps.setString(1, barcode);
		ps.setInt(2, toTerminal);

		rs = ps.executeQuery();

		if (rs.next()) {
			next_ticket = rs.getInt(1);
		} else {
			logger.log(Level.INFO, "Can not find next expected ticket information for terminal " + toTerminal + " and barcode " + barcode);
		}

		// return new roll information
		PaperTicketRoll roll = new PaperTicketRoll();
		roll.setBarcode(barcode);
		roll.setInitialized(true);
		roll.setTerminalid(toTerminal);
		roll.setNextTicket(next_ticket);

		return roll;
	}


	public static final int FIRST_BB_TICKET_NO = 1;
	public static final int BB_TICKET_SENTINEL = 1101;

	public static boolean processRollInit1(String terminal, String rollid, String servicecard, Connection conn) throws SQLException {

		// get paper ticket roll info
		PaperTicketRoll roll = RequestProcessor.getRoll(rollid, conn);

		if (roll == null) {
			logger.log(Level.SEVERE, "Roll initialization error: can not find " +
					        "paper ticket roll information for roll w/barcode : " + rollid);
			return false;
		} else {
			logger.log(Level.INFO, "Roll initialization: found roll info: barcode=" + roll.getBarcode() +
							"; terminal=" + roll.getTerminalid() + "; next ticket=" + roll.getNextTicket() + "; installed=" + roll.isInitialized());
		}

		// get terminal info
		TerminalInfo terminalInfo = RequestProcessor.getTerminalInfo(terminal, conn);

		if (terminalInfo == null) {
			logger.log(Level.SEVERE, "Roll initialization error: can not find terminal info for terminal " + terminal);
			return false;
		} else {
			logger.log(Level.INFO, "Roll initialization: found terminal info: name=" + terminalInfo.getName() +
							"; operday=" + terminalInfo.getOperday() + "; terminalid=" + terminalInfo.getTerminalid());
		}

		// if this roll already installed, move it to new terminal
		if (roll.isInitialized()) {
			// move the roll to a new terminal
			if (roll.getTerminalid() == terminalInfo.getTerminalid()) {
				logger.log(Level.INFO, "Roll initialization: roll already installed at terminal " + terminal + "(terminalid= " + terminalInfo.getTerminalid() + "); " +
											"next_ticket=" + roll.getNextTicket());
			} else {
				logger.log(Level.INFO, "Roll initialization: roll installed at terminal (terminalid=" + roll.getTerminalid() + "); " +
											"roll will be moved to terminal " + terminal + "(terminalid=" + terminalInfo.getTerminalid() + ")");
				roll = RequestProcessor.moveRoll(roll.getBarcode(), roll.getTerminalid(), terminalInfo.getTerminalid(), conn);
			}
		} else {
			// install the roll at new terminal
			logger.log(Level.INFO, "Roll initialization: roll is not installed in the system; roll will be installed on terminal " + terminal +
											" (terminalid=" + terminalInfo.getTerminalid() + "); next_ticket=1");
			roll = RequestProcessor.installRoll(roll.getBarcode(), terminalInfo.getTerminalid(), conn);
		}

		int cardid = RequestProcessor.getCardId(servicecard, conn);

		RequestProcessor.insertClubJournalRecord(cardid, ClubJournal.ROLL_INIT, terminalInfo.getTerminalid(), terminalInfo.getOperday(), conn);

		conn.commit();

		return true;
	}


	public static boolean processRollInit(String terminal, String rollid, String servicecard) {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			return RequestProcessor.processRollInit1(terminal, rollid, servicecard, conn);

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception while doing roll init", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch(Exception e) {}
		}

		return false;
	}


	private static int getCardId(String cardno, Connection conn) throws SQLException {
		int cardid = 0;

		PreparedStatement ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
		ps.setString(1, cardno);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			cardid = rs.getInt(1);
		}

		return cardid;
	}


	private static int getTerminalId(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID FROM TERMINAL WHERE HALLNAME = ?");

		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			return rs.getInt(1);
		} else {
			logger.log(Level.SEVERE, "Can not find terminal id for terminal " + terminal);
			throw new SQLException("Can not find terminal id for terminal " + terminal);
		}
	}


	public static boolean processPingRequest(String terminal) {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(true);

			PreparedStatement ps = conn.prepareStatement("SELECT COUNTTYPE FROM CASH_REGISTER WHERE REGISTERTYPE = 'MAIN'");

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				String counttype = rs.getString(1);
				if (!counttype.equals("DAILY")) {
					return false;
				}
			}

			ps = conn.prepareStatement("SELECT A.OPERDAY, B.OPERDAY FROM TERMINAL A, CASH_REGISTER B WHERE B.REGISTERTYPE = 'MAIN' AND A.HALLNAME = ?");
			ps.setString(1, terminal);

			rs = ps.executeQuery();

			if (rs.next()) {
				int terminaloperday = rs.getInt(1);
				int registeroperday = rs.getInt(2);

				if (terminaloperday > registeroperday) {
					return true;
				}
			}
		} catch(SQLException e) {
			logger.log(Level.SEVERE, "SQLException processing ping request from terminal " + terminal, e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (SQLException e) {}
		}

		return false;
	}



	private static boolean verifyTicketPrice(int price, String bbcode) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT LOTTERY_STATUS FROM NSI_20_JURIDICAL_LOTTERY WHERE NOMINAL = ? AND BB_CODE = ?");
		ps.setInt(1, price);
		ps.setString(2, bbcode);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			String status = rs.getString(1);

			if (status.equals("IN_WORK")) {
				return true;
			} else {
				logger.log(Level.SEVERE, "Terminal requested ticket for lottery in status " + status + " ticket price (" + price + ") for BB prefix " + bbcode);
			}
		} else {
			logger.log(Level.SEVERE, "Terminal requested wrong ticket price (" + price + ") for BB prefix " + bbcode);
		}

		return false;
	}
	
	private static boolean checkDoubleSale(String paperticket) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT PAPERTICKET FROM PAPERTICKET WHERE PAPERTICKET = ?");
		ps.setString(1, paperticket);

		return ps.executeQuery().next();
	}


	/**
	 * Purchase electronic lottery ticket and tie it to given paper ticket number
	 *
	 * @param terminal  	terminal name
	 * @param cardno    	card number
	 * @param game			game name
	 * @param lottery		lottery name
	 * @param price			ticket price
	 * @param paperticket	paper ticket number produced by lottery terminal
	 *
	 * @return	electronic lottery ticket
	 */
	public static BuyTicketResult processBuyLongBonusPaperTicketRequest(String terminal, String cardno, String game, String lottery, int price, String paperticket) {

		BuyTicketResult result = null;

		// process paper ticket; we will need to see if some paper tickets to be utilized
		PaperTicket bb = PaperTicket.parseTicket(paperticket);

		Connection conn = null;

		try {
			// get new connection; no auto-commit
			conn = ConnectionDispenser.getNewConnection(false);

			if (!verifyTicketPrice(price, bb.getLottery())) {
				throw new SQLException("Terminal requested ticket for lottery that does not exist: terminal " + terminal + "; game " + game + "; lottery " + lottery + "; price " + price + "; paperticket " + paperticket);
			}
			
			if (checkDoubleSale(paperticket)) {
				logger.log(Level.SEVERE, "Terminal requested paper ticket already sold: terminal " + terminal + " paperticket " + paperticket);
				result = new BuyTicketResult(null, 0, 0);
				result.setDoublesale(true);
				return result;
			}

			// get accounts
			String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			// get account balances - database query
			int ownBalance = TransactionProcessor.getAccountBalance(terminalAccnt);
			int winBalance = TransactionProcessor.getAccountBalance(terminalWinAccnt);

			// check that account balances are positive
			if (ownBalance < 0) {
				logger.log(Level.SEVERE, "OWN BANK balance is negative; account " + terminalAccnt + " has balance " + ownBalance);
				throw new SQLException("OWN BANK balance is negative: " + ownBalance);
			}

			if (winBalance < 0) {
				logger.log(Level.SEVERE, "WIN BANK balance is negative; account " + terminalWinAccnt + " has balance " + winBalance);
				throw new SQLException("WIN BANK balance is negative: " + winBalance);
			}

			// check that we have enough funds to purchase that ticket
			if ((ownBalance + winBalance) < price) {
				logger.log(Level.SEVERE, "BANK balance (" + (ownBalance + winBalance) + ") is less than ticket price (" + price + ")");
				throw new SQLException("BANK balance (" + (ownBalance + winBalance) + ") is less than ticket price (" + price + "); can not buy ticket with existing funds");
			}

			// get user id for card - another database query
			int userid = RequestProcessor.getUseridForCardno(cardno);

			// get terminal operday - database query
			int operday = RequestProcessor.getTerminalOperday(terminal);

			// get terminal id for this terminal - another database query
			int terminalid = RequestProcessor.getTerminalId(terminal);

			// get terminal denomination - another database query
			Denomination denom = RequestProcessor.getDenomination(terminal);

			// FIXME: all that needs to be put into a single method that would return TerminalInfo object
			PreparedStatement ps = conn.prepareStatement("SELECT A.BARCODE FROM BABINA A WHERE A.TERMINALID = ?");
			ps.setInt(1, terminalid);

			ResultSet rs = ps.executeQuery();

			String barcode = "";

			if (rs.next()) {
				barcode = rs.getString(1);
			} else {
				// this terminal's ticket roll has not been installed
				logger.log(Level.SEVERE, "Can not find any installed ticket rolls on this terminal (" + terminal + ")");
				throw new SQLException("Can not find any installed ticket rolls on this terminal (" + terminal + ")");
			}

			ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
			ps.setString(1, cardno);

			rs = ps.executeQuery();

			int cardid = 0;

			if (rs.next()) {
				cardid = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find player card (" + cardno + ") currently used on this terminal (" + terminal + ")");
				throw new SQLException("Can not find player card (" + cardno + ") currently used on this terminal (" + terminal + ")");
			}

			RequestProcessor.setTerminalGame(terminal, game);

			RequestProcessor.utilizePaperTickets(conn, terminalid, barcode, bb, price, operday, terminal, paperticket);


			// ATTENTION: table LONGTICKET is locked FOR UPDATE; access to table is serialized until we commit
			ps = conn.prepareStatement("SELECT TICKET, PERCENTAGE, ROUNDS, CONTENT FROM LONGTICKET WHERE LOTTERY = ? AND PRICE = ? AND STATUS = 1 LIMIT 1 FOR UPDATE");
			ps.setString(1, lottery);
			ps.setInt(2, price);

			rs = ps.executeQuery();

			int rounds = 0;
			String ticket = null;
			BorLongTicketVO vo = null;

			if (rs.next()) {
				ticket = rs.getString(1);
				int percentage = rs.getInt(2);
				rounds = rs.getInt(3);
				Blob blob = rs.getBlob(4);

				DataInputStream dis = new DataInputStream(new BufferedInputStream(blob.getBinaryStream()));

				vo = BorLongTicketVO.readTicket(dis);
				vo.setTicketNo(ticket);
				vo.setPercentage(percentage);

			} else {
				// no (unsold) ticket for this lottery and ticket price found in the database
				// >>>> at this point we are ok to release the lock

				RequestProcessor.updateTicketsAvailability(game, price, false);
				RequestProcessor.addLotteryLogRecord(userid, ticket, price, 0, 0, 0, operday, RequestProcessor.LLOG_NO_TIKT_AVAIL, terminalid);

				conn.commit();
				return null;
			}

			// transfer funds and convert them to credits according to denomination
			// NOTE: funds are transferred only if ticket can be found; this was the reason for a defect before
			if (ownBalance < price) {
				TransactionProcessor.recordConversion(terminalAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameCreditsAccnt, ownBalance, denom.getExchangeRate(), TransactionProcessor.BANKTOCREDITS_TRANSACTION, cardno, 10, "Lottery ticket buy transaction", operday);
				TransactionProcessor.recordConversion(terminalWinAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameWinCreditsAccnt, (price - ownBalance), denom.getExchangeRate(), TransactionProcessor.BANKTOCREDITS_TRANSACTION, cardno, 10, "Lottery ticket buy transaction", operday);
			} else {
				TransactionProcessor.recordConversion(terminalAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameCreditsAccnt, price, denom.getExchangeRate(), TransactionProcessor.BANKTOCREDITS_TRANSACTION, cardno, 10, "Lottery ticket buy transaction", operday);
			}

			int balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);
			int creditsbalance = TransactionProcessor.getAccountBalance(gameCreditsAccnt, gameWinCreditsAccnt);

			result = new BuyTicketResult(vo, balance, creditsbalance);

			ps = conn.prepareStatement("UPDATE LONGTICKET SET STATUS = 2, ROUNDS = ? WHERE TICKET = ?");
			ps.setInt(1, rounds + 1);
			ps.setString(2, ticket);

			ps.executeUpdate();

			// logging the paper ticket
			ps = conn.prepareStatement("INSERT INTO PAPERTICKET(TICKET, PAPERTICKET, BABINA) VALUES(?, ?, ?)");
			ps.setString(1, ticket);
			ps.setString(2, paperticket);
			ps.setString(3, barcode);

			ps.executeUpdate();

			// >>>> at this point we are OK to release the lock
			// commit all the information we needed for selling the ticket
			// "FOR UPDATE" database lock is released
			conn.commit();

			logger.log(Level.INFO, "Player at lottery terminal " + terminal + " purchased long ticket: " + ticket);

			// logging part

			RequestProcessor.insertTicketlogTicketPurchasedRecord(ticket, price, operday);

			RequestProcessor.addLotteryLogRecord(userid, ticket, price, 0, 0, 0, operday, RequestProcessor.LLOG_TICKET_PURCHASE, terminalid);

			RequestProcessor.insertPlayercardLogTicketPurchasedRecord(cardid, ticket, price, balance, operday);

			conn.commit();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing bonus ticket purchase request", e);
			try {
				conn.rollback();
			} catch(SQLException sqle) {}
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {}
			ConnectionDispenser.releaseConnection();
		}

		return result;
	}


	private static void insertTicketlogTicketPurchasedRecord(String ticket, int price, int operday) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("INSERT INTO TICKET_LOG (TICKET, PURCHASED, PRICE, OPERDAY) VALUES(?, NOW(), ?, ?)");
		ps.setString(1, ticket);
		ps.setInt(2, price);
		ps.setInt(3, operday);

		ps.executeUpdate();
	}


	private static void insertPlayercardLogTicketPurchasedRecord(int cardid, String ticket, int price, long balance, int operday) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

		ps.setInt(1, cardid);
		ps.setInt(2, 1);
		ps.setInt(3, 1);
		ps.setString(4, ticket);
		ps.setInt(5, price);
		ps.setInt(6, 0);
		ps.setLong(7, balance);
		ps.setInt(8, operday);
		ps.setInt(9, 0);

		ps.executeUpdate();
	}



	private static void utilizePaperTickets(Connection conn, int terminalid, String barcode, PaperTicket bb, int price, int operday, String terminal, String paperticket) throws SQLException {
		// get next expected ticket for this terminal

		PreparedStatement ps = conn.prepareStatement("SELECT A.NEXT_TICKET, A.BARCODE FROM BB_TRACKER A WHERE A.TERMINAL = ?");
		ps.setInt(1, terminalid);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int next_ticket = rs.getInt(1);
			String roll_barcode = rs.getString(2);

			logger.log(Level.INFO, "Found barcode assigned to this terminal: " + roll_barcode + "; barcode received from terminal is " + barcode);

			if (roll_barcode.equals(barcode)) {
				// the roll barcode matches; let's see if next expected ticket matches
				if (next_ticket == bb.getTicketno()) {
					// everything is OK; nothing to utilize
					logger.log(Level.INFO, "Barcode and next expected number ticket matches");

				} else {

					if (bb.getTicketno() < next_ticket) {
						logger.log(Level.SEVERE, "Expected ticket number is higher than the one received from terminal; expected = " + next_ticket + "; received from terminal = " + bb.getTicketno());
					}

					logger.log(Level.INFO, "Utilizing tickets from " + next_ticket + " to " + bb.getTicketno());

					for (int i = next_ticket; i < bb.getTicketno(); i++) {
						// utilize all tickets here

						ps = conn.prepareStatement("INSERT INTO UTILIZED_TICKETS (RECNUM, RECTIME, SERIES, TICKET, PRICE, OPERDAY, TERMINAL) VALUES(NULL, NOW(), ?, ?, ?, ?, ?)");
						ps.setString(1, bb.getSeries());
						ps.setString(2, bb.getTicketprefix() + String.format("%04d", i));
						ps.setInt(3, price);	// price
						ps.setInt(4, operday);	// operday
						ps.setInt(5, terminalid);	// terminal id

						ps.executeUpdate();

						ps = conn.prepareStatement("INSERT INTO UTILIZED_TICKETS_STATS(RECNUM, RECTIME, PAPERTICKET, OPERDAY, TERMINAL) VALUES (NULL, NOW(), ?, ?, ?)");

						ps.setString(1, paperticket.substring(0, paperticket.length() - 4) + String.format("%04d", i));
						ps.setInt(2, operday);
						ps.setInt(3, terminalid);

						ps.executeUpdate();
					}

					logger.log(Level.INFO, "Updating BB tracker for barcode " + roll_barcode + " for terminal " + terminal + "; setting next expected ticket to " + (bb.getTicketno()+1));

				}

				ps = conn.prepareStatement("UPDATE BB_TRACKER A SET A.NEXT_TICKET = ? WHERE A.TERMINAL = ?");
				ps.setInt(1, bb.getTicketno() + 1);
				ps.setInt(2, terminalid);

				ps.executeUpdate();
			} else {
				logger.log(Level.SEVERE, "Roll id does not match roll id initialized on this terminal: received = " + barcode + "; stored = " + roll_barcode);
			}
		} else {
			// this terminal does not have barcode for the ticket roll specified
			logger.log(Level.SEVERE, "Can not find BB_TRACKER information for paper ticket no " + paperticket + " on terminal " + terminal);
		}
	}


	public static BorLongTicketVO processBuyLongBonusTicketRequest(String terminal, String cardno, String game, String lottery, int price) {

		BorLongTicketVO vo = null;
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			int ownBalance = TransactionProcessor.getAccountBalance(terminalAccnt);
			int winBalance = TransactionProcessor.getAccountBalance(terminalWinAccnt);

			if (ownBalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDITS balance is negative");
				throw new SQLException("OWN CREDITS balance is negative: " + ownBalance);
			}

			if ((ownBalance + winBalance) < price) {
				logger.log(Level.SEVERE, "CREDITS balance (" + ownBalance + ") is less than ticket price (" + price + ")");
				throw new SQLException("CREDITS balance (" + ownBalance + ") is less than ticket price (" + price + ")");
			}

			int operday = RequestProcessor.getTerminalOperday(terminal);

			String ticket = null;

			int userid = RequestProcessor.getUseridForCardno(cardno);
			int terminalid = RequestProcessor.getTerminalId(terminal);

			Denomination denom = RequestProcessor.getDenomination(terminal);

			// ATTENTION: table LONGTICKET is locked FOR UPDATE; access to table is serialized until we commit
			PreparedStatement ps = conn.prepareStatement("SELECT TICKET, PERCENTAGE, CONTENT FROM LONGTICKET WHERE LOTTERY = ? AND PRICE = ? AND STATUS = 1 LIMIT 1 FOR UPDATE");
			ps.setString(1, lottery);
			ps.setInt(2, price);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				ticket = rs.getString(1);
				int percentage = rs.getInt(2);
				Blob blob = rs.getBlob(3);
				DataInputStream dis = new DataInputStream(new BufferedInputStream(blob.getBinaryStream()));
				vo = BorLongTicketVO.readTicket(dis);
				vo.setTicketNo(ticket);
				vo.setPercentage(percentage);
			} else {
				// no (unsold) ticket for this lottery and ticket price found in the database

				RequestProcessor.updateTicketsAvailability(game, price, false);
				RequestProcessor.addLotteryLogRecord(userid, ticket, price, 0, 0, 0, operday, RequestProcessor.LLOG_NO_TIKT_AVAIL, terminalid);

				conn.commit();
				return null;
			}

			ps = conn.prepareStatement("UPDATE LONGTICKET SET STATUS = 2, ROUNDS = ROUNDS + 1 WHERE TICKET = ?");
			ps.setString(1, ticket);

			ps.executeUpdate();

			ps = conn.prepareStatement("INSERT INTO TICKET_LOG (TICKET, PURCHASED, PRICE, OPERDAY) VALUES(?, NOW(), ?, ?)");
			ps.setString(1, ticket);
			ps.setInt(2, price);
			ps.setInt(3, operday);

			int res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Can not insert ticket purchase information into ticket log");
				throw new SQLException("Can not insert ticket purchase information into ticket log");
			}

			ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
			ps.setString(1, cardno);

			rs = ps.executeQuery();

			int cardid = 0;

			if (rs.next()) {
				cardid = rs.getInt(1);
			}

			RequestProcessor.addLotteryLogRecord(userid, ticket, price, 0, 0, 0, operday, RequestProcessor.LLOG_TICKET_PURCHASE, terminalid);

			RequestProcessor.setTerminalGame(terminal, game);

			if (ownBalance < price) {
				TransactionProcessor.recordConversion(terminalAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameCreditsAccnt, ownBalance, denom.getExchangeRate(), TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
				TransactionProcessor.recordConversion(terminalWinAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameWinCreditsAccnt, (price - ownBalance), denom.getExchangeRate(), TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			} else {
				TransactionProcessor.recordConversion(terminalAccnt, denom.getCurrencyAccount(), denom.getCreditsAccount(), gameCreditsAccnt, price, denom.getExchangeRate(),
						TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			}

			int balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);


			ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, cardid);
			ps.setInt(2, 1);
			ps.setInt(3, 1);
			ps.setString(4, ticket);
			ps.setInt(5, price);
			ps.setInt(6, 0);
			ps.setLong(7, balance);
			ps.setInt(8, operday);
			ps.setInt(9, 0);

			res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Failed to insert record into player card log");
				throw new SQLException("Failed to insert record into player card log");
			}

			// "FOR UPDATE" database lock is released
			conn.commit();

			logger.log(Level.INFO, "Player at lottery terminal " + terminal + " purchased long ticket: " + ticket);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing bonus ticket purchase request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (SQLException e) {}
		}

		return vo;
	}


	public static String setParam(String terminal, String param, String value, String servicecard) throws SQLException {

		if (param == null) {
			logger.log(Level.SEVERE, "PARM_SET parameter name is NULL");
			return null;
		}

		if (param.equals("terminal.denomination")) {

			int denomination = Integer.parseInt(value);

			Connection conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("UPDATE TERMINAL SET DENOMINATION = ? WHERE HALLNAME = ?");
			ps.setInt(1, denomination);
			ps.setString(2, terminal);

			ps.executeUpdate();

			int location = RequestProcessor.getTerminalId(terminal);
			int operday = RequestProcessor.getTerminalOperday(terminal);

			int cardid = RequestProcessor.getCardId(servicecard, conn);

			switch(denomination) {
			case 10:
				RequestProcessor.insertClubJournalRecord(cardid, 210, location, operday, conn);

				ps = conn.prepareStatement("INSERT INTO SECURITY_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, USERNAME, EVENTTYPE, LOCATION, CONTENT, OPERDAY) " +
																	"VALUES(NULL, NOW(), ?, NULL, NULL, ?, 0, ?, ?)");
				ps.setString(1, terminal);
				ps.setInt(2, 20008);
				ps.setString(3, "Изменение деноминации на 0,1 UAH на терминале " + terminal);
				ps.setInt(4, operday);

				ps.executeUpdate();

				break;

			case 20:
				RequestProcessor.insertClubJournalRecord(cardid, 220, location, operday, conn);

				ps = conn.prepareStatement("INSERT INTO SECURITY_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, USERNAME, EVENTTYPE, LOCATION, CONTENT, OPERDAY) " +
																	"VALUES(NULL, NOW(), ?, NULL, NULL, ?, 0, ?, ?)");
				ps.setString(1, terminal);
				ps.setInt(2, 20008);
				ps.setString(3, "Изменение деноминации на 0,2 UAH на терминале " + terminal);
				ps.setInt(4, operday);

				ps.executeUpdate();

				break;

			default:
				logger.log(Level.SEVERE, "Undefined denomination: " + denomination);

				ps = conn.prepareStatement("INSERT INTO SECURITY_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, USERNAME, EVENTTYPE, LOCATION, CONTENT, OPERDAY) " +
																	"VALUES(NULL, NOW(), ?, NULL, NULL, ?, 0, ?, ?)");
				ps.setString(1, terminal);
				ps.setInt(2, 20008);
				ps.setString(3, "Изменение деноминации на неизвестную (" + denomination + " КОП) на терминале " + terminal);
				ps.setInt(4, operday);

				ps.executeUpdate();


			}

			conn.commit();

			return value;

		} else if (param.equals("server.version")) {

		} else if (param.equals("terminal.BNA.hardware")) {

		} else if (param.equals("terminal.cardreader.firmware")) {

		} else if (param.equals("terminal.device.configuration")) {

		} else {
			logger.log(Level.SEVERE, "Unknown PARM_GET param name: " + param);
			return null;
		}

		return null;

	}

	public static void putParams(String terminal, String[] names, String[] values, String servicecard) {

		try {

			for (int i = 0; i < names.length; i++) {

				if (names[i] == null || names[i].isEmpty()) {
					logger.log(Level.SEVERE, "Parameter name is empty in position " + i + " in PARM_PUT");
				} else {
					setParam(terminal, names[i], values[i], servicecard);
				}
			}
		}
		catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception trying to save param value", e);
		}

	}


	public static String getParam(String terminal, String param) {

		if (param == null) {
			logger.log(Level.SEVERE, "PARM_GET parameter name is NULL");
			return null;
		}

		try {

			if (param.equals("terminal.denomination")) {

				Connection conn = ConnectionDispenser.getNewConnection(false);

				PreparedStatement ps = conn.prepareStatement("SELECT DENOMINATION FROM TERMINAL WHERE HALLNAME = ?");
				ps.setString(1, terminal);

				ResultSet rs = ps.executeQuery();

				if (rs.next()) {
					int denomination = rs.getInt(1);
					return "" + denomination;
				} else {
					logger.log(Level.SEVERE, "Missing denomination value for terminal " + terminal);
					return null;
				}
			} else if (param.equals("server.version")) {

			} else if (param.equals("terminal.BNA.hardware")) {

			} else if (param.equals("terminal.cardreader.firmware")) {

			} else if (param.equals("terminal.device.configuration")) {

			} else {
				logger.log(Level.SEVERE, "Unknown PARM_GET param name: " + param);
				return null;
			}

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception trying to get param value", e);
		}

		return null;
	}





	public static Denomination getDenomination(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT DENOMINATION FROM TERMINAL WHERE HALLNAME = ?");
		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int denomination = rs.getInt(1);

			ps = conn.prepareStatement("SELECT DESCRIPTION, EXCHANGERATE, CURRENCYACCOUNT, CREDITSACCOUNT FROM DENOMINATION WHERE DENOMINATION = ?");
			ps.setInt(1, denomination);

			rs = ps.executeQuery();

			if (rs.next()) {
				Denomination denom = new Denomination();
				denom.setDenomination(denomination);
				denom.setDescription(rs.getString(1));
				denom.setExchangeRate(rs.getInt(2));
				denom.setCurrencyAccount(rs.getString(3));
				denom.setCreditsAccount(rs.getString(4));

				return denom;
			} else {
				logger.log(Level.SEVERE, "Can not find denomination details for terminal " + terminal);
				throw new SQLException("Can not find denomination details for terminal " + terminal);
			}
		} else {
			logger.log(Level.SEVERE, "Can not find terminal denomination information for terminal " + terminal);
			throw new SQLException("Can not find terminal denomination information for terminal " + terminal);
		}
	}



	private static void updateTicketsAvailability(String game, int price, boolean available) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE AVAILABILITY SET AVAILABLE = ? WHERE GAME = ? AND PRICE = ?");

		ps.setBoolean(1, available);
		ps.setString(2, game);
		ps.setInt(3, price);

		int res = ps.executeUpdate();

		if (res < 1) {
			logger.log(Level.SEVERE, "Can not update game availability for " + game + ", price = " + price);
		} else {
			logger.log(Level.INFO, "Updated game availability for " + game + ", price = " + price);
		}

	}


	public static boolean processPrintConfirm(String terminal, String ticket, long win) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(true);

			PreparedStatement ps = conn.prepareStatement("UPDATE TICKET_LOG A, PAPERTICKET B, LOTTERY_LOG C, TERMINAL D SET C.RECEIPTPRINTED = TRUE WHERE " +
					"D.HALLNAME = ? AND B.PAPERTICKET = ? AND A.TICKET = B.TICKET AND C.TICKETNO = A.TICKET AND C.TERMINAL = D.TERMINALID AND C.RECORDTYPE = 5");
			ps.setString(1, terminal);
			ps.setString(2, ticket);

			int res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Can not find played out ticket for terminal " + terminal + " and ticket " + ticket);
			}

			return true;

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception caught", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return false;
	}


	public static PrintRequestResult processPrintRequest(String terminal, String ticket, long win) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(true);

			PreparedStatement ps = conn.prepareStatement("SELECT A.TOTALWIN, A.CASHEDOUT FROM TICKET_LOG A, PAPERTICKET B, LOTTERY_LOG C, TERMINAL D WHERE " +
					"D.HALLNAME = ? AND B.PAPERTICKET = ? AND A.TICKET = B.TICKET AND C.TICKETNO = A.TICKET AND C.TERMINAL = D.TERMINALID AND C.RECORDTYPE = 5");
			ps.setString(1, terminal);
			ps.setString(2, ticket);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				long totalwin = rs.getLong(1);
				long timestamp = rs.getTimestamp(2).getTime();

				if (totalwin != win) {
					logger.log(Level.SEVERE, "Discrepancy in total win printed on ticket: terminal " + terminal + " data: " + win + "; server data: " + totalwin);
				}

				return new PrintRequestResult(true, timestamp, totalwin);
			} else {
				logger.log(Level.SEVERE, "Can not find completed ticket " + ticket + " at terminal " + terminal);
				return new PrintRequestResult(false, 0L, 0L);
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception caught", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new PrintRequestResult(false, 0L, 0L);
	}


	public static BonusRequestResult processBonusRequest(String terminal, String game, String pin, int[] results) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String bankOwnAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String bankWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			String creditsOwnAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String creditsWinAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			String creditsGameAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);			// CREDITS account (player's own credits)
			String lottoFundAccnt = RequestProcessor.getLotteryFundAccountForGame(game);					// Lottery winning fund account
			String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);	// Game escrow account

			int operday = RequestProcessor.getTerminalOperday(terminal);
			int ownbalance = TransactionProcessor.getAccountBalance(creditsGameAccnt);

			if (ownbalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDIT account balance is negative");
				throw new SQLException("OWN CREDIT account balance is negative: " + ownbalance);
			}

			int win = results[0];

			if (win > 0) {
				// Credit game escrow account with bonus win amount
				TransactionProcessor.recordTransaction(lottoFundAccnt, gameEscrowAccnt, win, TransactionProcessor.GAMEWIN_TRANSACTION, "PIN: " + pin, 10, "Player bonus win escrow transaction", operday);
			} else if (win < 0) {
				logger.log(Level.SEVERE, "WIN amount is negative: " + win);
				throw new SQLException("WIN amount is negative: " + win);
			}

			long bankBalance = TransactionProcessor.getAccountBalance(bankOwnAccnt, bankWinAccnt);
			long creditsBalance = TransactionProcessor.getAccountBalance(creditsOwnAccnt, creditsWinAccnt);
			long winBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			conn.commit();

			return new BonusRequestResult(true, bankBalance, creditsBalance, winBalance);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new BonusRequestResult(false, 0L, 0L, 0L);
	}




	public static LongTicketVO processBuyLongTicketRequest(String terminal, String cardno, String game, String lottery, int amount) {

		LongTicketVO vo = null;
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			int ownBalance = TransactionProcessor.getAccountBalance(terminalAccnt);
			if (ownBalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDITS account balance is negative");
				throw new SQLException("OWN CREDITS account balance is negative: " + ownBalance);
			}

			String ticket = null;
			int userid = RequestProcessor.getUseridForCardno(cardno);
			int terminalid = RequestProcessor.getTerminalId(terminal);

			// FIXME: hint optimizer to use index LONGTICKET (LOTTERY, PRICE, STATUS)
			PreparedStatement ps = conn.prepareStatement("SELECT TICKET, CONTENT FROM LONGTICKET WHERE LOTTERY = ? AND PRICE = ? AND STATUS = 1 LIMIT 1 FOR UPDATE");
			ps.setString(1, lottery);
			ps.setInt(2, amount);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {

				ticket = rs.getString(1);
				Blob blob = rs.getBlob(2);
				DataInputStream dis = new DataInputStream(new BufferedInputStream(blob.getBinaryStream()));
				vo = LongTicketVO.readTicket(dis).setTicketNo(ticket);

			} else {
				// no (unsold) ticket for this lottery and ticket price found in the database

				RequestProcessor.updateTicketsAvailability(game, amount, false);
				RequestProcessor.addLotteryLogRecord(userid, ticket, amount, 0, 0, 0, operday, RequestProcessor.LLOG_NO_TIKT_AVAIL, terminalid);

				conn.commit();
				return null;
			}

			ps = conn.prepareStatement("UPDATE LONGTICKET SET STATUS = 2, ROUNDS = ROUNDS + 1 WHERE TICKET = ?");
			ps.setString(1, ticket);
			ps.setString(2, lottery);

			ps.executeUpdate();

			RequestProcessor.addLotteryLogRecord(userid, ticket, amount, 0, 0, 0, operday, RequestProcessor.LLOG_TICKET_PURCHASE, terminalid);

			if (ownBalance < amount) {
				TransactionProcessor.recordTransaction(terminalAccnt, gameCreditsAccnt, ownBalance, TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
				TransactionProcessor.recordTransaction(terminalWinAccnt, gameWinCreditsAccnt, (amount - ownBalance), TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			} else {
				TransactionProcessor.recordTransaction(terminalAccnt, gameCreditsAccnt, amount, TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			}

			conn.commit();

			logger.log(Level.INFO, "Long ticket bought: " + ticket);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return vo;
	}


	public static int getBankAccountBalance(String terminal, String game) {

		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);
			balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught getting account balance", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return balance;
	}


	public static int getCreditsAccountBalance(String terminal, String game) {

		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String creditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String creditsWinAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
			balance = TransactionProcessor.getAccountBalance(creditsAccnt, creditsWinAccnt);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught getting CREDITS account balance", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return balance;
	}

	public static int processBuyTicketRequest(String terminal, String game, int amount) {
		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String gameCreditsAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String gameWinCreditsAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);
			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			int ownBalance = TransactionProcessor.getAccountBalance(terminalAccnt);
			if (ownBalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDITS account balance is negative");
				throw new SQLException("OWN CREDITS account balance is negative: " + ownBalance);
			}

			if (ownBalance < amount) {
				TransactionProcessor.recordTransaction(terminalAccnt, gameCreditsAccnt, ownBalance, TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
				TransactionProcessor.recordTransaction(terminalWinAccnt, gameWinCreditsAccnt, (amount - ownBalance), TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			} else {
				TransactionProcessor.recordTransaction(terminalAccnt, gameCreditsAccnt, amount, TransactionProcessor.BANKTOCREDITS_TRANSACTION, "Card user", 10, "Lottery ticket buy transaction", operday);
			}

			balance = TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt);

			conn.commit();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Can not process deposit request; exception caught", e);
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Can not process deposit request; exception caught", e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Can not process deposit request; exception caught", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return balance;
	}


	public static int getGamePercentage(String terminal, String game) {

		Connection conn = null;
		int percentage = 92;

		String lottery = GameRegistry.getInstance().getLotteries().get(game);

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT PERCENTAGE FROM GAMEREELS WHERE GAMENAME = ?");
			ps.setString(1, lottery);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				percentage = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find game info for game " + game + " in the database");
				throw new SQLException("Can not find game info for game " + game + " in the database");
			}

			return percentage;

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving game percentage", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return percentage;
	}

	public static byte[] getGameReels(String terminal, String game, int percentage) {

		Connection conn = null;
		byte[] reels = null;

		String lottery = GameRegistry.getInstance().getLotteries().get(game);

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT GAMEID FROM GAMEREELS WHERE GAMENAME = ? AND PERCENTAGE = ?");
			ps.setString(1, lottery);
			ps.setInt(2, percentage);

			ResultSet rs = ps.executeQuery();

			int gameid = -1;

			if (rs.next()) {
				gameid = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find game info for game " + game + " and percentage " + percentage + " in the database");
				throw new SQLException("Can not find game info for game " + game + " and percentage " + percentage + " in the database");
			}

			ps = conn.prepareStatement("SELECT REELS FROM REELSET WHERE GAMEID = ? AND PERCENTAGE = ?");
			ps.setInt(1, gameid);
			ps.setInt(2, percentage);

			rs = ps.executeQuery();

			while (rs.next()) {
				Blob content = rs.getBlob(1);
				long length = content.length();
				reels = content.getBytes(1, (int)length);

				logger.log(Level.INFO, "Retrieved reels content for game " + game + " and percentage " + percentage);
			}

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving game percentage", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return reels;
	}


	public static byte[] getGameReels(String terminal, String game) {

		Connection conn = null;
		byte[] reels = null;

		String lottery = GameRegistry.getInstance().getLotteries().get(game);

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT GAMEID FROM GAMEREELS WHERE GAMENAME = ?");
			ps.setString(1, lottery);

			ResultSet rs = ps.executeQuery();

			int gameid = -1;

			if (rs.next()) {
				gameid = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find game info for game " + game + " in the database");
				throw new SQLException("Can not find game info for game " + game + " in the database");
			}

			ps = conn.prepareStatement("SELECT REELS FROM REELSET WHERE GAMEID = ?");
			ps.setInt(1, gameid);

			rs = ps.executeQuery();

			while (rs.next()) {
				Blob content = rs.getBlob(1);
				long length = content.length();
				reels = content.getBytes(1, (int)length);

				logger.log(Level.INFO, "Retrieved reels content for game " + game);
			}

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving game percentage", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return reels;
	}

	public static int[][] getGameRegularReels(String terminal, String game) {

		Connection conn = null;
		int[][] reels = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT GAMEID FROM GAMEREELS WHERE GAMENAME = ?");
			ps.setString(1, game);

			ResultSet rs = ps.executeQuery();

			int gameid = -1;

			if (rs.next()) {
				gameid = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find game info for game " + game + " in the database");
				throw new SQLException("Can not find game info for game " + game + " in the database");
			}

			ps = conn.prepareStatement("SELECT REELNO, STOPNO, SYMBOL FROM REELSET WHERE GAMEID = ? AND REELTYPE = 'REGULAR' ORDER BY REELNO, STOPNO");
			ps.setInt(1, gameid);

			rs = ps.executeQuery();

			while (rs.next()) {

			}

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving game percentage", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return reels;
	}

	public static int[][] getGameBonusReels(String terminal, String game) {

		Connection conn = null;
		int[][] reels = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT GAMEID FROM GAMEREELS WHERE GAMENAME = ?");
			ps.setString(1, game);

			ResultSet rs = ps.executeQuery();

			int gameid = -1;

			if (rs.next()) {
				gameid = rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "Can not find game info for game " + game + " in the database");
				throw new SQLException("Can not find game info for game " + game + " in the database");
			}

			ps = conn.prepareStatement("SELECT REELNO, STOPNO, SYMBOL FROM REELSET WHERE GAMEID = ? AND REELTYPE = 'BONUS' ORDER BY REELNO, STOPNO");
			ps.setInt(1, gameid);

			rs = ps.executeQuery();

			while (rs.next()) {

			}

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving game percentage", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return reels;
	}


	private static boolean insertClubJournalRecord(int cardid, int opsid, int location, int operday, Connection conn) throws SQLException {

		PreparedStatement ps = conn.prepareStatement("INSERT INTO CLUB_JOURNAL(RECID, RECTIME, CARDID, OPSID, LOCATION, OPERDAY) VALUES (NULL, NULL, ?, ?, ?, ?)");
		ps.setInt(1, cardid);
		ps.setInt(2, opsid);
		ps.setInt(3, location);
		ps.setInt(4, operday);

		ps.executeUpdate();

		return true;

	}


	public static int processCashoutRequest(String terminal, String pin) {
		Connection conn = null;
		int balance = 0;
		int winbalance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);
			String pinAccnt = RequestProcessor.getAccountForPinNo(pin);
			String pinWinAccnt = RequestProcessor.getWinAccountForPinNo(pin);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			int croperday = RequestProcessor.getCashRegisterOperday();

			balance = TransactionProcessor.getAccountBalance(terminalAccnt);
			if (balance < 0) {
				logger.log(Level.SEVERE, "OWN BANK account balance is negative");
				throw new SQLException("OWN BANK account balance is negative: " + balance);
			}

			winbalance = TransactionProcessor.getAccountBalance(terminalWinAccnt);
			if (winbalance < 0) {
				logger.log(Level.SEVERE, "WIN BANK account balance is negative");
				throw new SQLException("WIN BANK account balance is negative: " + winbalance);
			}

			TransactionProcessor.recordTransaction(terminalAccnt, pinAccnt, balance, TransactionProcessor.CASHOUT_TRANSACTION, "Card user", 10, "Cashout transaction", operday);
			TransactionProcessor.recordTransaction(terminalWinAccnt, pinWinAccnt, winbalance, TransactionProcessor.CASHOUT_TRANSACTION, "Card user", 10, "Cashout transaction", operday);

			balance = TransactionProcessor.getAccountBalance(pinAccnt, pinWinAccnt);
			if (balance < 0) {
				logger.log(Level.SEVERE, "PIN account balance is negative");
				throw new SQLException("PIN account balance is negative: " + balance);
			}

			RequestProcessor.setTerminalState(terminal, Terminal.IDLE_STATE);

			String bnaAccnt = RequestProcessor.getAccountForTerminalBNACash(terminal);

			int bnabalance = TransactionProcessor.getAccountBalance(bnaAccnt);

			PreparedStatement ps = conn.prepareStatement("SELECT COUNTTYPE FROM CASH_REGISTER WHERE REGISTERTYPE = 'MAIN'");

			ResultSet rs = ps.executeQuery();

			String counttype = "EXTRA";

			if (rs.next()) {
				counttype = rs.getString(1);
			}

			if (bnabalance == 0 && counttype.equals("DAILY")) {

				logger.log(Level.INFO, "Cashout completed in DAILY count mode; setting terminal " + terminal + " to BLOCKED state");

				ps = conn.prepareStatement("UPDATE TERMINAL SET OPERDAY =? WHERE HALLNAME = ?");
				ps.setInt(1, croperday +1);
				ps.setString(2, terminal);

				ps.executeUpdate();

				RequestProcessor.setTerminalState(terminal, Terminal.BLOCKED_STATE);
			}

			ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
			ps.setString(1, pin);

			rs = ps.executeQuery();

			int cardid = 0;

			if (rs.next()) {
				cardid = rs.getInt(1);
			}

			ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, NULL, ?, ?, ?, ?, ?)");
			ps.setInt(1, cardid);
			ps.setInt(2, 2);		// 3 - misc transaction
			ps.setInt(3, 104);		// 93 - card is removed from terminal
			ps.setInt(4, 0);		// debit - 0
			ps.setInt(5, 0);	// credit - deposit amount
			ps.setLong(6, balance);	// balance - card balance
			ps.setInt(7, operday);	// current operday for that terminal
			ps.setInt(8, 0);		// register number not available

			int res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Failed to insert record into player card log");
				throw new SQLException("Failed to insert record into player card log");
			}

			ps = conn.prepareStatement("UPDATE SESSION_LOG SET END = NOW(), STATUS='CLOSED' WHERE TERMINAL=? AND CARDNO=? AND STATUS='ACTIVE'");
			ps.setString(1, terminal);
			ps.setString(2, pin);

			res = ps.executeUpdate();

			conn.commit();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Exception caught while processing deposit request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return balance;

	}


	public static DepositRequestResult processDepositRequest(String terminal, String game, String ticket, int playseq) {

		// return DepositRequestProcessor.processDepositRequest(terminal, game, ticket, playseq);

		Connection conn = null;
		int balance = 0;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			logger.log(Level.INFO, "Processing deposit request for terminal " + terminal + "; game : " + game);

			String ownBankAccnt = RequestProcessor.getAccountForTerminal(terminal);							// BANK account (player's own funds)
			String winBankAccnt = RequestProcessor.getWinAccountForTerminal(terminal);						// BANK account (player's win funds)

			String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);

			String creditsGameAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String creditsWinGameAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			int amount = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			if (amount < 0) {
				logger.log(Level.SEVERE, "ESCROW account balance is negative");
				throw new SQLException("ESCROW account balance is negative: " + amount);
			}

			logger.log(Level.INFO, terminal + " : Current balance for ESCROW account = " + amount);

			int operday = RequestProcessor.getTerminalOperday(terminal);

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


	public static int getRoleForCard(String pin, String terminal) {

		Connection conn = null;
		int role = -1;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT A.ROLE, B.USERSTATE FROM ROLES_FOR_USERS A, USERS B, CARD C WHERE A.USER = B.USERID AND B.CARDID = C.CARDID AND C.CARDNO = ?");
			ps.setString(1, pin);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				role = rs.getInt(1);
				String blocked = rs.getString(2);

				if (blocked.equals("BLOCKED")) {
					role = -1;
				}

			} else {
				role = -1;
			}

			ps = conn.prepareStatement("SELECT STATE FROM TERMINAL WHERE HALLNAME=?");
			ps.setString(1, terminal);

			rs = ps.executeQuery();

			if (rs.next()) {
				String state = rs.getString(1);

				if (state.equals("BLOCKED") && role == 1) {
					role = -2;
				}
			}

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while geting role using pin", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return role;
	}

	public static void recordStatusMessage(String terminal, String log) {

		if (terminal == null) return;

		Connection conn = null;

		try {
			// logger.log(Level.INFO, "Logging STATUS NOTIFICATION: " + terminal + " : " + log);

			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT CLUB_ID FROM NSI_20_CLUB");

			ResultSet rs = ps.executeQuery();

			int clubid = 0;

			if (rs.next()) {
				clubid = rs.getInt(1);

				// logger.log(Level.INFO, "Logging STATUS NOTIFICATION: club id = " + clubid);
			}

			ps = conn.prepareStatement("SELECT ASSETID FROM TERMINAL WHERE SERIALNO = ?");
			ps.setString(1, terminal);

			rs = ps.executeQuery();

			String assetid = "";

			if (rs.next()) {
				assetid = rs.getString(1);
			}

			ps = conn.prepareStatement("INSERT INTO STATUS_NOTIFICATION (CLUBID, CLUB, REGISTER) VALUES (?, '', ?)");
			ps.setInt(1, clubid);
			ps.setString(2, assetid + " (" + terminal + ") : " + log);

			ps.executeUpdate();

			conn.commit();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Can not record status notification for terminal " + terminal, e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (Exception e) {}
		}
	}


	public static boolean recordFraudAlert(String terminal, String log) {

		Connection conn = null;

		try {
			// logger.log(Level.INFO, "Logging FRAUD ALERT: " + terminal + " : " + log);

			conn = ConnectionDispenser.getNewConnection(true);

			PreparedStatement ps = conn.prepareStatement("SELECT OPERDAY FROM CLUB_OPERDAY");

			ResultSet rs = ps.executeQuery();

			int operday = 0;
			if (rs.next()) {
				operday = rs.getInt(1);
			}


			ps = conn.prepareStatement("INSERT INTO FRAUD_ALERT(TERMINAL, MSG, DISPLAYED, OPERDAY) VALUES(?, ?, FALSE, ?)");
			ps.setString(1, terminal);
			ps.setString(2, log);
			ps.setInt(3, operday);

			ps.executeUpdate();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Can not record fraud alert for terminal " + terminal, e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (Exception e) {}
		}

		return true;
	}


	public static String getCountType(String terminal) {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT A.COUNTTYPE FROM CASH_REGISTER A, TERMINAL B WHERE A.OPERDAY = B.OPERDAY AND A.REGISTERTYPE = 'MAIN' AND B.HALLNAME = ?");
			ps.setString(1, terminal);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				String counttype = rs.getString(1);
				return counttype;
			} else {
				return "EXTRA";
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Can not retrieve count type for terminal " + terminal, e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (Exception e) {}
		}

		return "DAILY";
	}

	/**
	 * Add record to terminal log (SECURITY_LOG table)
	 *
	 * @param terminal   terminal name
	 * @param event      event category id
	 * @param log        log record
	 *
	 * @throws SQLException  thrown if database eror happens
	 */
	public static void addLogRecord(String terminal, int event, String log) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		int operday = RequestProcessor.getTerminalOperday(terminal);

		PreparedStatement ps = conn.prepareStatement("INSERT INTO SECURITY_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, USERNAME, EVENTTYPE, LOCATION, CONTENT, OPERDAY) " +
																				"VALUES(NULL, NOW(), ?, NULL, NULL, ?, 0, ?, ?)");
		ps.setString(1, terminal);
		ps.setInt(2, event);
		ps.setString(3, log);
		ps.setInt(4, operday);

		ps.executeUpdate();
	}


	/**
	 * Add record to terminal log (SECURITY_LOG table)
	 *
	 * @param terminal   terminal name
	 * @param event      event category id
	 * @param log        log record
	 *
	 * @throws SQLException  thrown if database eror happens
	 */
	public static void addTLogRecord(String terminal, int event, String log) {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			PreparedStatement ps = conn.prepareStatement("INSERT INTO SECURITY_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, USERNAME, EVENTTYPE, LOCATION, CONTENT, OPERDAY) " +
																"VALUES(NULL, NOW(), ?, NULL, NULL, ?, 0, ?, ?)");
			ps.setString(1, terminal);
			ps.setInt(2, event);
			ps.setString(3, log);
			ps.setInt(4, operday);

			ps.executeUpdate();

			conn.commit();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while adding TLog record", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}
	}


	private static int getUseridForCardno(String cardno) throws SQLException {

		int userid = 0;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareCall("SELECT A.USERID FROM USERS A, CARD B WHERE A.CARDID = B.CARDID AND B.CARDNO = ?");

		ps.setString(1, cardno);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			userid = rs.getInt(1);
		}

		rs.close();

		return userid;
	}

	public static boolean closeActiveTerminalSession(String terminal, String servicecard) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("UPDATE SESSION_LOG SET END = NOW(), STATUS='CLOSED' WHERE TERMINAL=? AND CARDNO=? AND STATUS='ACTIVE'");
			ps.setString(1, terminal);
			ps.setString(2, servicecard);

			ps.executeUpdate();

			ps.executeUpdate();

			conn.commit();

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception processing authentication tech request", e);
			return false;
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return true;
	}


	public static boolean processAuthenticationTechRequest(String terminal, String pin, int role) {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			PreparedStatement ps = conn.prepareStatement("INSERT INTO SESSION_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, SESSION, START, END, STATUS, OPERDAY) VALUES (NULL, NOW(), ?, ?, ?, NOW(), NOW(), 'ACTIVE', ?)");
			ps.setString(1, terminal);
			ps.setString(2, pin);
			ps.setString(3, terminal + "|" + System.currentTimeMillis());
			ps.setInt(4, operday);

			ps.executeUpdate();

			int cardid = RequestProcessor.getCardId(pin, conn);

			int location = RequestProcessor.getTerminalId(terminal);

			if (role == 2) {
				RequestProcessor.insertClubJournalRecord(cardid, 121, location, operday, conn);
			} else if (role == 3) {
				RequestProcessor.insertClubJournalRecord(cardid, 122, location, operday, conn);
			} else if (role == 4 || role == 99) {
				RequestProcessor.insertClubJournalRecord(cardid, 123, location, operday, conn);
			}

			conn.commit();

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception processing authentication tech request", e);
			return false;
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return true;
	}


	public static AuthenticationResult processAuthenticationPINRequest(String terminal, String pin) {
		Connection conn = null;
		AuthenticationResult auth = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String terminalAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String pinAccount = RequestProcessor.getAccountForPinNo(pin);
			String terminalWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);
			String pinWinAccount = RequestProcessor.getWinAccountForPinNo(pin);

			auth = RequestProcessor.performAuthentication(pin, terminal, 10);

			int operday = RequestProcessor.getTerminalOperday(terminal);

			int balance = TransactionProcessor.getAccountBalance(pinAccount);
			if (balance < 0) {
				logger.log(Level.SEVERE, "OWN PIN account balance is negative");
				throw new SQLException("OWN PIN account balance is negative: " + balance);
			}

			int winbalance = TransactionProcessor.getAccountBalance(pinWinAccount);
			if (winbalance < 0) {
				logger.log(Level.SEVERE, "WIN PIN account balance is negative");
				throw new SQLException("WIN PIN account balance is negative: " + winbalance);
			}

			if (auth.isAuthenticated()) {

				Denomination denomination = RequestProcessor.getDenomination(terminal);

				auth.setDenomination(denomination);

				TransactionProcessor.recordTransaction(pinAccount, terminalAccnt, balance, TransactionProcessor.TERMINALLOAD_TRANSACTION, "Card user", 10, "Initial funds load from PIN", operday);
				TransactionProcessor.recordTransaction(pinWinAccount, terminalWinAccnt, winbalance, TransactionProcessor.TERMINALLOAD_TRANSACTION, "Card user", 10, "Initial funds load from PIN", operday);

				auth.setAmount(TransactionProcessor.getAccountBalance(terminalAccnt, terminalWinAccnt));

				RequestProcessor.setTerminalState(terminal, Terminal.INPLAY_STATE);
				RequestProcessor.setTerminalState(terminal, Terminal.INPLAY_STATE, pin);

				PreparedStatement ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
				ps.setString(1, pin);

				ResultSet rs = ps.executeQuery();

				int cardid = 0;

				if (rs.next()) {
					cardid = rs.getInt(1);
				}

				ps = conn.prepareStatement("INSERT INTO PLAYERCARD_LOG(RECNUM, RECTIME, CARDID, RECTYPE, DESCR, TICKET, DEBIT, CREDIT, BALANCE, OPERDAY, REGISTER) VALUES(NULL, NOW(), ?, ?, ?, NULL, ?, ?, ?, ?, ?)");
				ps.setInt(1, cardid);
				ps.setInt(2, 2);		// 2 - misc transaction
				ps.setInt(3, 103);		// 93 - card is inserted into terminal
				ps.setInt(4, 0);		// debit - 0
				ps.setInt(5, 0);		// credit - 0
				ps.setLong(6, balance);	// balance - card balance
				ps.setInt(7, operday);	// current operday for that terminal
				ps.setInt(8, 0);		// register number not available

				int res = ps.executeUpdate();

				if (res < 1) {
					logger.log(Level.SEVERE, "Failed to insert record into player card log");
					throw new SQLException("Failed to insert record into player card log");
				}

				ps = conn.prepareStatement("INSERT INTO SESSION_LOG(RECORDNO, RECORDTS, TERMINAL, CARDNO, SESSION, START, END, STATUS, OPERDAY) VALUES (NULL, NOW(), ?, ?, ?, NOW(), NOW(), 'ACTIVE', ?)");
				ps.setString(1, terminal);
				ps.setString(2, pin);
				ps.setString(3, terminal + "|" + System.currentTimeMillis());
				ps.setInt(4, operday);

				res = ps.executeUpdate();

			} else {
				// TODO: send authentication error
			}

			conn.commit();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing authentication request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return auth;
	}


	private static AuthenticationResult performAuthentication(String pin, String terminal, int location) throws Exception {
		AuthenticationResult auth = null;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT A.USERNAME FROM USERS A, CARD B WHERE A.CARDID = B.CARDID AND CARDNO = ? AND STATE='ACTIVE'");
		ps.setString(1, pin);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			// we might need to do something here
		} else {

			auth = new AuthenticationResult(pin, 0, false, false);
			// RequestProcessor.addLogRecord(terminal, RequestProcessor.SECURITY_EVENT_FAILED_LOGIN, "Failed login attempt");
			return auth;
		}

		rs.close();
		ps.close();

		ps = conn.prepareStatement("SELECT HALLNAME FROM TERMINAL WHERE CARDNO = ?");
		ps.setString(1, pin);
		
		rs = ps.executeQuery();
		
		if (rs.next()) {
			String oldterminal = rs.getString(1);
			logger.log(Level.INFO, "Attempt to authenticate with card " + pin + " on terminal " + terminal + " when card is still in terminal " + oldterminal);

			auth = new AuthenticationResult(pin, 0, false, false);
			return auth;
		}

		// RequestProcessor.addLogRecord(terminal, RequestProcessor.SECURITY_EVENT_LOGIN_ATTEMP, "Login attempt");

		String pinAccount = RequestProcessor.getAccountForPinNo(pin);

		ps = conn.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ACCOUNTNO = ?");
		ps.setString(1, pinAccount);

		rs = ps.executeQuery();

		if (rs.next()) {
			int amount = rs.getInt(1);

			long special_card_number = Long.decode("0x" + pin).longValue();

			logger.log(Level.INFO, "Checking special card status for card # " + pin );

			ps = conn.prepareStatement("SELECT NSI_20_SPECIAL_CARD_CLUB.SPECIAL_CARD_STATUS, NSI_20_SPECIAL_CARD_CLUB.SPECIAL_CARD_ID " +
					"FROM NSI_20_SPECIAL_CARD_CLUB, NSI_20_CARD " +
					"WHERE NSI_20_SPECIAL_CARD_CLUB.CARD_NUMBER = NSI_20_CARD.CARD_NUMBER AND NSI_20_CARD.CARD_ID = ?");
			
			ps.setLong(1, special_card_number);

			rs = ps.executeQuery();

			boolean isSpecialCard = false;

			long specialCardId = 0L;
			long clubId = 0L;

			if (rs.next()) {
				String special_card_status = rs.getString(1);
				specialCardId = rs.getLong(2);
				if (special_card_status.equals("IN_WORK")) {
					isSpecialCard = true;
					logger.log(Level.INFO, "Special card status set to IN_WORK in NSI; card # " + pin);
				} else {
					logger.log(Level.INFO, "Special card status is NOT set to IN_WORK in NSI; card # " + pin);
				}
			}

			ps = conn.prepareStatement("SELECT CLUB_ID FROM NSI_20_CLUB");

			rs = ps.executeQuery();

			if (rs.next()) {
				clubId = rs.getLong(1);
			}

			String drawingUrl = null;

			ps = conn.prepareStatement("SELECT URL FROM LGS_URL");

			rs = ps.executeQuery();

			if (rs.next()) {
				drawingUrl = rs.getString(1);
			}

			auth = new AuthenticationResult(pin, amount, true, isSpecialCard);

			if (isSpecialCard) {
				auth.setSpecialCardId(specialCardId);
				auth.setClubId(clubId);
				auth.setDrawingUrl(drawingUrl);

				logger.log(Level.INFO, "Special card id = " + specialCardId + "; clubId = " + clubId + " drawingUrl = " + drawingUrl);
			}
		} else {
			logger.log(Level.SEVERE, "No account found for PIN no: " + pin);
			auth = new AuthenticationResult(pin, 0, false, false);
			return auth;
		}

		// RequestProcessor.addLogRecord(terminal, RequestProcessor.SECURITY_EVENT_SUCCESFUL_LOGIN, "Login successful");

		return auth;
	}


	public static void turnoffTerminal(String terminal) {
		Connection conn = null;

		logger.log(Level.INFO, "Turning off terminal " + terminal + " ...");

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			RequestProcessor.setTerminalState(terminal, Terminal.UNAVAILABLE_STATE);

			conn.commit();

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception trying to set terminal state to TURNED OFF", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch(SQLException e) {}
		}
	}


	public static PlayRequestResult processPlayLongTicketRequest(String terminal, String game, String pin, String ticket, String visualization, int bet, int lines, int win, int playseq) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			// get account numbers:

			String ownBankAccnt = RequestProcessor.getAccountForTerminal(terminal);							// BANK account (player's own funds)
			String winBankAccnt = RequestProcessor.getWinAccountForTerminal(terminal);						// BANK account (player's win funds)

			String creditsGameAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);			// CREDITS account (player's own credits)
			String creditsWinGameAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);		// CREDITS account (player's win credits)

			String lottoProceedsAccnt = RequestProcessor.getLotteryProceedsAccountForGame(game);			// Lottery proceeds account
			String lottoFundAccnt = RequestProcessor.getLotteryFundAccountForGame(game);					// Lottery winning fund account

			String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);	// Game escrow account


			int operday = RequestProcessor.getTerminalOperday(terminal);
			int ownbalance = TransactionProcessor.getAccountBalance(creditsGameAccnt);
			if (ownbalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDIT account balance is negative");
				throw new SQLException("OWN CREDIT account balance is negative: " + ownbalance);
			}

			if (bet <= 0) {
				logger.log(Level.SEVERE, "BET amount is not positive");
				throw new SQLException("BET amount is not positive : " + bet);
			}

			if (lines <= 0) {
				logger.log(Level.SEVERE, "LINES number is not positive");
				throw new SQLException("LINES number is not positive : " + lines);
			}

			if (bet*lines > 0) {
				// Deduct total bet amount from player's CREDITS account(s):
				if (ownbalance < bet*lines) {
					// we have to split the amount; player's own credits do not cover the whole amount
					TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, ownbalance, TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy segment transaction", operday);
					TransactionProcessor.recordTransaction(creditsWinGameAccnt, lottoProceedsAccnt, (bet*lines - ownbalance), TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy segment transaction", operday);
				} else {
					// player have enough of his own credits to cover the bet
					TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, bet*lines, TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy segment transaction", operday);
				}
			}

			if (win > 0) {
				// Credit game escrow account with win amount
				TransactionProcessor.recordTransaction(lottoFundAccnt, gameEscrowAccnt, win, TransactionProcessor.GAMEWIN_TRANSACTION, "PIN: " + pin, 10, "Lottery escrow win transaction", operday);
			} else if (win < 0) {
				logger.log(Level.SEVERE, "WIN amount is negative: " + win);
				throw new SQLException("WIN amount is negative: " + win);
			}

			Denomination denomination = RequestProcessor.getDenomination(terminal);

			PreparedStatement ps = conn.prepareStatement("INSERT INTO PLAY_LOG(TICKET, BETTIME, DOUBLEUP, PLAYSEQ, BET, LINESPLAYED, VISUALIZATION, DENOMINATION) VALUES(?, NOW(), 1, ?, ?, ?, ?, ?)");

			ps.setString(1, ticket);
			ps.setInt(2, playseq);
			ps.setInt(3, bet);
			ps.setInt(4, lines);
			ps.setString(5, visualization);
			ps.setInt(6, denomination.getDenomination());

			int res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Can not insert play segment record into play log");
				throw new SQLException("Can not insert play segment record into play log");
			}

			long bankBalance = TransactionProcessor.getAccountBalance(ownBankAccnt, winBankAccnt);
			long creditsBalance = TransactionProcessor.getAccountBalance(creditsGameAccnt, creditsWinGameAccnt);
			long winBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			conn.commit();

			return new PlayRequestResult(true, bankBalance, creditsBalance, winBalance);
		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing PlayLongTicket request", e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Exception caught while processing PlayLongTicket request", e);
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Exception caught while processing PlayLongTicket request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new PlayRequestResult(false, 0L, 0L, 0L);
	}


	public static PlayRequestResult processPlayBonusLongTicketRequest(String terminal, String game, String pin, int bet, int win) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			// get account numbers:

			String ownBankAccnt = RequestProcessor.getAccountForTerminal(terminal);							// BANK account (player's own funds)
			String winBankAccnt = RequestProcessor.getWinAccountForTerminal(terminal);						// BANK account (player's win funds)

			String creditsGameAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);			// CREDITS account (player's own credits)
			String creditsWinGameAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);		// CREDITS account (player's win credits)

			String lottoProceedsAccnt = RequestProcessor.getLotteryProceedsAccountForGame(game);			// Lottery proceeds account
			String lottoFundAccnt = RequestProcessor.getLotteryFundAccountForGame(game);					// Lottery winning fund account

			String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);	// Game escrow account


			int operday = RequestProcessor.getTerminalOperday(terminal);
			int ownbalance = TransactionProcessor.getAccountBalance(creditsGameAccnt);
			int winbalance = TransactionProcessor.getAccountBalance(creditsWinGameAccnt);

			// check if account balance is negative; most likely database is corrupted
			if (ownbalance < 0) {
				logger.log(Level.SEVERE, "OWN CREDIT account balance is negative");
				throw new SQLException("OWN CREDIT account balance is negative: " + ownbalance);
			}

			// check if we have enough money to make a bet
			if ((ownbalance + winbalance) < bet) {
				logger.log(Level.SEVERE, "CREDITS balance (" + (ownbalance + winbalance) + " cr)does not have enough credits to bet " + bet + " cr");
				throw new SQLException("CREDITS balance (" + (ownbalance + winbalance) + " cr)does not have enough credits to bet " + bet + " cr");
			}

			if (bet > 0) {
				// If we actually bet something, deduct total bet amount from player's CREDITS account(s):
				// however, this should never happen (bonus games are free)?????????

				if (ownbalance < bet) {
					// we have to split the amount; player's own credits do not cover the whole amount
					TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, ownbalance, TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy bonus segment transaction", operday);
					TransactionProcessor.recordTransaction(creditsWinGameAccnt, lottoProceedsAccnt, (bet - ownbalance), TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy bonus segment transaction", operday);
				} else {
					// player have enough of his own credits to cover the bet
					TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, bet, TransactionProcessor.GAMEPLAY_TRANSACTION, "PIN: " + pin, 10, "Player buy segment transaction", operday);
				}
			}

			if (win >= 0) {
				// Credit game escrow account with win amount
				TransactionProcessor.recordTransaction(lottoFundAccnt, gameEscrowAccnt, win, TransactionProcessor.GAMEWIN_TRANSACTION, "PIN: " + pin, 10, "Player buy segment trasnaction", operday);
			} else if (win < 0) {
				logger.log(Level.SEVERE, "WIN amount is negative: " + win);
				throw new SQLException("WIN amount is negative: " + win);
			}

			long bankBalance = TransactionProcessor.getAccountBalance(ownBankAccnt, winBankAccnt);
			long creditsBalance = TransactionProcessor.getAccountBalance(creditsGameAccnt) + TransactionProcessor.getAccountBalance(creditsWinGameAccnt);
			long winBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			conn.commit();

			return new PlayRequestResult(true, bankBalance, creditsBalance, winBalance);

		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception caught while processing PlayLongTicket request", e);
			try {
				conn.rollback();
			} catch (SQLException sqle) {}

		} finally {
			try {
				conn.close();
			}
			catch (SQLException e) {}
			ConnectionDispenser.releaseConnection();
		}

		// return false if request can not be processed

		return new PlayRequestResult(false, 0L, 0L, 0L);
	}


	public static LotteryGameResult processPlayRequest(String terminalId, String gameId, int bet, int[] lines) {

		Connection conn = null;
		LotteryGameResult gameResult = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

					String creditsGameAccnt = RequestProcessor.getCreditsAccountForGame(terminalId, gameId);
					String creditsWinGameAccnt = RequestProcessor.getWinCreditsAccountForGame(terminalId, gameId);
					String lottoProceedsAccnt = RequestProcessor.getLotteryProceedsAccountForGame(gameId);
					String lottoFundAccnt = RequestProcessor.getLotteryFundAccountForGame(gameId);
					String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminalId, gameId);

					int amountToBet = lines.length*bet;
					int operday = RequestProcessor.getTerminalOperday(terminalId);

					int ownbalance = TransactionProcessor.getAccountBalance(creditsGameAccnt);
					if (ownbalance < 0) {
						logger.log(Level.SEVERE, "OWN CREDITS account balance is negative");
						throw new SQLException("OWN CREDITS account balance is negative");
					}

					if (ownbalance < amountToBet) {
						int tx_bet = TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, ownbalance, TransactionProcessor.GAMEPLAY_TRANSACTION, "Card player", 10, "Play request trasnaction", operday);
						TransactionProcessor.recordTransaction(creditsWinGameAccnt, lottoProceedsAccnt, (amountToBet - ownbalance), TransactionProcessor.GAMEPLAY_TRANSACTION, "Card player", 10, "Play request trasnaction", operday);
						RequestProcessor.addPlayLogRecord(terminalId, gameId, bet, lines, tx_bet, operday);
					} else {
						int tx_bet = TransactionProcessor.recordTransaction(creditsGameAccnt, lottoProceedsAccnt, amountToBet, TransactionProcessor.GAMEPLAY_TRANSACTION, "Card player", 10, "Play request trasnaction", operday);
						RequestProcessor.addPlayLogRecord(terminalId, gameId, bet, lines, tx_bet, operday);
					}

					gameResult = RequestProcessor.getLotteryGameResult("Card player", 10, gameId);

					if (gameResult.getWin() < 0) {
						logger.log(Level.SEVERE, "WIN amount is negative");
						throw new SQLException("WIN amount is negative");
					}

					int tx_win = TransactionProcessor.recordTransaction(lottoFundAccnt, gameEscrowAccnt, gameResult.getWin(), TransactionProcessor.GAMEWIN_TRANSACTION, "Card player", 10, "Play request trasnaction", operday);
					RequestProcessor.addWinLogRecord(conn, terminalId, gameId, gameResult, tx_win);

					conn.commit();
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception caught while trying to process play request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return gameResult;
	}


	private static int addWinLogRecord(Connection conn, String terminal, String game, LotteryGameResult gameResult, int tx) throws TransactionException {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("INSERT INTO WIN_LOG VALUES (NULL, NULL, ?, ?, ?, ?, ?)");

			ps.setString(1, terminal);
			ps.setString(2, game);

			GameStops stops = gameResult.getStops();
			stops.getStops();

			if (ps.executeUpdate() < 1) {
				throw new TransactionException("Can not add child record to PLAY_RESPONSE_LOG");
			}

			return 1;
		} catch (SQLException sqle) {
			throw new TransactionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException sqle) {
				// Ignore
			}
		}
	}

	private static int addPlayLogRecord(String terminal, String game, int bet, int[] lines, int tx, int operday) throws TransactionException {
		PreparedStatement ps = null;

		try {
			Connection conn = ConnectionDispenser.getConnection();

			ps = conn.prepareStatement("INSERT INTO BET_LOG VALUES (NULL, NULL, ?, ?, ?, ?, ?, ?)");

			ps.setString(1, terminal);
			ps.setString(2, game);
			ps.setInt(3, bet);
			ps.setInt(4, lines.length);
			ps.setInt(5, tx);
			ps.setInt(6, operday);

			if (ps.executeUpdate() != 1) {
				throw new TransactionException("Can not add child record to PLAY_REQUEST_LOG");
			}

			return 1;
		} catch (SQLException sqle) {
			throw new TransactionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException sqle) {
				// Ignore
			}
		}
	}

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

	// ---- terminal handling methods -------

	public static String getTerminalState(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT STATE FROM TERMINAL WHERE HALLNAME = ?");

		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			return rs.getString(1);
		} else {
			logger.log(Level.SEVERE, "Can not find terminal information for terminal " + terminal);
			throw new SQLException("Can not find terminal information for terminal " + terminal);
		}
	}

	public static void setTerminalGame(String terminal, String game) throws SQLException {

		if (terminal == null || terminal.equals("")) return;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE TERMINAL SET GAME = ? WHERE HALLNAME = ?");

		ps.setString(1, game);
		ps.setString(2, terminal);

		int res = ps.executeUpdate();

		if (res != 1) {
			logger.log(Level.SEVERE, "Can not update game for terminal " + terminal);
			throw new SQLException("Can not update game for terminal " + terminal);
		}
	}

	public static void setTerminalState(String terminal, String state, String pin) throws SQLException {

		if (terminal == null || terminal.equals("") || state == null) return;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE TERMINAL SET STATE = ?, CARDNO = ? WHERE HALLNAME = ?");

		ps.setString(1, state);
		ps.setString(2, pin);
		ps.setString(3, terminal);

		ps.executeUpdate();

		logger.log(Level.INFO, "Terminal " + terminal + " is set to state " + state);
	}

	public static void setTerminalState(String terminal, String state) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		if (terminal == null || terminal.equals("") || state == null) return;

		PreparedStatement ps = conn.prepareStatement("UPDATE TERMINAL SET STATE = ?, CARDNO = NULL WHERE HALLNAME = ?");

		ps.setString(1, state);
		ps.setString(2, terminal);

		int res = ps.executeUpdate();

		if (res != 1) {
			logger.log(Level.SEVERE, "Can not update state for terminal " + terminal);
			throw new SQLException("Can not update terminal state for terminal " + terminal);
		}

		if (state.equals(Terminal.IDLE_STATE)) {

			ps = conn.prepareStatement("UPDATE TERMINAL, CASH_REGISTER, COUNTS SET TERMINAL.OPERDAY = CASH_REGISTER.OPERDAY WHERE " +
					"TERMINAL.OPERDAY < CASH_REGISTER.OPERDAY AND CASH_REGISTER.REGISTERTYPE = 'MAIN' AND  TERMINAL.HALLNAME = ?");

			ps.setString(1, terminal);

			ps.executeUpdate();
		}

		logger.log(Level.INFO, "Terminal " + terminal + " state changed to " + state);
	}


	private static LotteryGameResult getLotteryGameResult(String user, int location, String game)
	    throws SQLException, NoLotteryTicketFoundException, StopsNotFoundException, LotteryLogFailedException, NoTicketFoundException {

		Connection conn = ConnectionDispenser.getConnection();

		// NOTE: this is for short ticket only

		PreparedStatement ps = conn.prepareStatement("SELECT TICKETSEQNO, TICKETNO, WIN, PRICE, LOTTERYID, BATCHID FROM LOTTERY_TICKET WHERE STATE = 1 ORDER BY TICKETSEQNO LIMIT 1 FOR UPDATE");

		LotteryGameResult result = null;

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int ticketSeqNo = rs.getInt(1);
			String ticketNo = rs.getString(2);
			@SuppressWarnings("unused")
			int lotteryId = rs.getInt(3);
			@SuppressWarnings("unused")
			int batchId = rs.getInt(4);
			@SuppressWarnings("unused")
			int price = rs.getInt(5);
			int win = rs.getInt(6);
			@SuppressWarnings("unused")
			int state = rs.getInt(7);

			rs.close();
			ps.close();

			int[] stops = generateStops(game, win);

			GameStops gameStops = new GameStops(5);
			gameStops.setStops(stops);

			result = new LotteryGameResult(gameStops, win, "WIN_ONLY", ticketNo);

			changeTicketStatus(ticketSeqNo, 2);

			addLotteryLogRecord(result);
		} else {
			throw new NoLotteryTicketFoundException("Can not find lottery ticket in the database", game);
		}

		return result;
	}


	/**
	 * @param conn
	 * @param ticketSeqNo
	 * @param newTicketStatus
	 * @throws SQLException
	 */
	private static void changeTicketStatus(int ticket, int status) throws SQLException, NoTicketFoundException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE LOTTERY_TICKET SET STATE = ? WHERE TICKETSEQNO = ?");

		ps.setInt(1, ticket);
		ps.setInt(2, status);

		if (ps.executeUpdate() != 1) {
			throw new NoTicketFoundException(ticket);
		}

		ps.close();
	}


	/**
	 * @param conn
	 * @param result
	 * @throws SQLException
	 */
	private static void addLotteryLogRecord(LotteryGameResult result) throws SQLException, LotteryLogFailedException {

		// PreparedStatement ps = conn.prepareStatement("INSERT INTO LOTTERY_LOG(RECORDNO, RECORDTS, TICKETNO, AMOUNT, USERID, BET, CELLNO, WIN, OPERDAY, RECORDTYPE, RECEIPTPRINTED, RECEIPTNO, TERMINAL) " +
		// 		"VALUES (NULL, NOW(), ?, ?, ?, ?, ?)");

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("INSERT INTO LOTTERY_LOG " +
																"VALUES (NULL, NOW(), ?, ?, ?, ?, ?)");

		if (result != null) {
			ps.setString(1, result.getTicket());
		} else {
			ps.setString(1, "");
		}
		ps.setString(2, "12345678");
		ps.setString(3, "Card user");
		ps.setInt(4, 1);
		ps.setInt(5, 10);

		if (ps.executeUpdate() != 1) {
			throw new LotteryLogFailedException();
		}

		ps.close();
	}


	private static int[] generateStops(String game, int win) throws SQLException, StopsNotFoundException {
		int[] stops;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT STOP1, STOP2, STOP3, STOP4, STOP5 FROM GAME_REELS WHERE WIN = ? AND GAME = ? ORDER BY RAND() LIMIT 1");
		ps.setInt(1, win);
		ps.setString(2, game);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			stops = new int[5];
			stops[0] = rs.getInt(1);
			stops[1] = rs.getInt(2);
			stops[2] = rs.getInt(3);
			stops[3] = rs.getInt(4);
			stops[4] = rs.getInt(5);
		} else {
			throw new StopsNotFoundException(win);
		}

		rs.close();
		ps.close();

		return stops;
	}


	/**
	 * Records the completed count for a terminal. Returns newly assigned count id.
	 *
	 * @param terminalid
	 * @return integer containing count id
	 */
	public static void recordEndCount(String terminalid) throws SQLException {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			RequestProcessor.setTerminalState(terminalid, Terminal.IDLE_STATE);

			conn.commit();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing count request", e);
			throw e;
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (SQLException e) {}
		}
	}



	public static void performEmergencyCount(String terminal) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			BanknoteCount[] banknoteCount = null;

			banknoteCount = fetchBanknoteCount(terminal);

			int total = 0;

			for (BanknoteCount item : banknoteCount) {
				total += item.getCount() * item.getDenomination();
			}

			// if count is zero, do not record the count
			if (total == 0) return;

			int countid = processBanknoteCount(terminal, total, "EMERGENCY", "");

			recordEmergencyCount(terminal, total, countid);

			conn.commit();

		} catch(LoggingException e) {
			logger.log(Level.SEVERE, "Exception caught while processing emergency count request", e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Exception caught while processing emergency count request", e);
		} catch(AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Exception caught while processing emergency count request", e);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing emergency count request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}
	}


	public static int recordEmergencyCount(String terminalid, int total, int countid) throws SQLException, LoggingException, TransactionException, AccountNotFoundException {

		String bnaCashAccount = RequestProcessor.getAccountForTerminalBNACash(terminalid);
		String pinCashAccount = "TERM.EMERGENCY.COUNT." + terminalid;
		int operday = RequestProcessor.getTerminalOperday(terminalid);

		logger.log(Level.INFO, "Recording transaction for BNA emergency count: bna account: " + bnaCashAccount + "; administrator card account: " + pinCashAccount);

		int txId = TransactionProcessor.recordTransaction(bnaCashAccount, pinCashAccount, total, TransactionProcessor.BNACOUNT_TRANSACTION, "SYSTEM", 0, "Emergency terminal count", operday);

		Connection conn = ConnectionDispenser.getConnection();

		int terminal = RequestProcessor.getTerminalId(terminalid);

		PreparedStatement ps = conn.prepareStatement("SELECT A.CARDID FROM CARD A, USERS B, CASH_REGISTER C WHERE A.CARDID = B.CARDID AND B.USERID = C.USERID AND C.REGISTERTYPE = 'MAIN'");

		ResultSet rs = ps.executeQuery();

		int cardid = 0;

		if (rs.next()) {
			cardid = rs.getInt(1);
		}

		RequestProcessor.insertClubJournalRecord(cardid,
				ClubJournal.EMERGENCY_COUNT, terminal, operday, conn);

		return txId;
	}


	public static BanknoteCount[] performTerminalCount(String terminalid, String cardNumber, String countMode) throws SQLException{

		Connection conn = null;
		BanknoteCount[] banknoteCount = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			banknoteCount = fetchBanknoteCount(terminalid);

			int total = 0;

			for (BanknoteCount item : banknoteCount) {
				total += item.getCount() * item.getDenomination();
			}

			int terminalOperday = getTerminalOperday(terminalid);
			int registerOperday = getCashRegisterOperday();

			// To fix runaway operday
			if (terminalOperday > registerOperday) {
				logger.log(Level.INFO, "Runaway register operday; changing count type to EXTRA");
				countMode = "EXTRA";
			}

			if (total == 0) {
				if (countMode.equals("DAILY")) {
					logger.log(Level.SEVERE, "Updating terminal operday to the next one");
					updateTerminalOperday(terminalid);
				}

				conn.commit();

				return null;
			}

			int countid = processBanknoteCount(terminalid, total, countMode, cardNumber);

			if (countMode.equals("DAILY")) {
				logger.log(Level.SEVERE, "Updating terminal operday to the next one");
				updateTerminalOperday(terminalid);
			}

			recordTerminalCount(terminalid, cardNumber, countMode, total, countid);

			conn.commit();

		} catch(LoggingException e) {
			logger.log(Level.SEVERE, "Exception caught while processing count request", e);
			throw new SQLException(e);
		} catch(TransactionException e) {
			logger.log(Level.SEVERE, "Exception caught while processing count request", e);
			throw new SQLException(e);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing count request", e);
			throw e;
		} catch (AccountNotFoundException e) {
			logger.log(Level.SEVERE, "Exception caught while processing count request", e);
			throw new SQLException(e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return banknoteCount;
	}


	public static void updateTerminalOperday(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE TERMINAL SET OPERDAY = OPERDAY + 1 WHERE HALLNAME = ?");

		ps.setString(1, terminal);

		ps.executeUpdate();

		ps = conn.prepareStatement("SELECT MAX(A.OPERDAYID), B.OPERDAY FROM OPERDAY A, TERMINAL B WHERE B.HALLNAME = ?");

		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int operdaySys = rs.getInt(1);
			int operdayTerm = rs.getInt(2);

			if (operdayTerm > operdaySys) {
				ps = conn.prepareStatement("INSERT INTO OPERDAY VALUES(?, SYSDATE(), 1)");
				ps.setInt(1, operdayTerm);
			}
		}
	}


	public static int recordTerminalCount(String terminalid, String cardNumber, String countMode, int total, int countid) throws SQLException, LoggingException, TransactionException, AccountNotFoundException {

		String bnaCashAccount = RequestProcessor.getAccountForTerminalBNACash(terminalid);
		String pinCashAccount = RequestProcessor.getAccountForPinNoCash(cardNumber);
		Connection conn = ConnectionDispenser.getConnection();
		int operday = RequestProcessor.getTerminalOperday(terminalid);

		logger.log(Level.INFO, "Recording transaction for BNA count: bna account: " + bnaCashAccount + "; administrator card account: " + pinCashAccount);

		int txId = TransactionProcessor.recordTransaction(bnaCashAccount, pinCashAccount, total, TransactionProcessor.BNACOUNT_TRANSACTION, cardNumber, 0, countMode + " terminal count", operday);

		int terminal = RequestProcessor.getTerminalId(terminalid);

		PreparedStatement ps = conn.prepareStatement("SELECT CARDID FROM CARD WHERE CARDNO = ?");
		ps.setString(1, cardNumber);

		ResultSet rs = ps.executeQuery();

		int cardid = 0;

		if (rs.next()) {
			cardid = rs.getInt(1);
		}

		if (countMode.equals("DAILY")) {

			RequestProcessor.insertClubJournalRecord(cardid, ClubJournal.DAILY_COUNT, terminal, operday, conn);

		} else if (countMode.equals("EXTRA")) {

			RequestProcessor.insertClubJournalRecord(cardid, ClubJournal.EXTRA_COUNT, terminal, operday, conn);

		} else {
			logger.log(Level.SEVERE, "Can not find count mode for : " + countMode);
		}

		return txId;

	}


	public static int processBanknoteCount(String terminalid, int total, String mode, String cardnumber) throws SQLException {

		logger.log(Level.INFO, "Starting to process BNA count for terminal " + terminalid);

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID, OPERDAY FROM TERMINAL WHERE HALLNAME= ?");

		ps.setString(1, terminalid);

		ResultSet rs = ps.executeQuery();

		int terminalno = 0;
		int operday = 0;

		while (rs.next()) {
			terminalno = rs.getInt(1);
			operday = rs.getInt(2);

			logger.log(Level.INFO, "Processing banknote count for terminal " + terminalid + " (termina id = " + terminalno + ")");
		}

		PreparedStatement createCountRecord = conn.prepareStatement("INSERT INTO COUNTS (TERMINAL, TOTALAMT, OPERDAY, COUNTTYPE, ACTUALAMT, COMPLETED, CARDNO) VALUES (?, ?, ?, ?, NULL, 0, ?)", Statement.RETURN_GENERATED_KEYS);

		createCountRecord.setInt(1, terminalno);
		createCountRecord.setInt(2, total);
		createCountRecord.setInt(3, operday);
		createCountRecord.setString(4, mode);
		createCountRecord.setString(5, cardnumber);

		int recordsInserted = createCountRecord.executeUpdate();

		if (recordsInserted != 1) {
			logger.log(Level.SEVERE, "Can not insert new terminal count into the database");
		}

		ResultSet countInfo = createCountRecord.getGeneratedKeys();

		int countno = 0;

		if (countInfo.next()) {
			countno = countInfo.getInt(1);
		}

		logger.log(Level.INFO, "Processed BNA count; created new count id: " + countno);

		PreparedStatement updateCountEntries = conn.prepareStatement("UPDATE COUNT_ENTRY SET COUNT = ? WHERE TERMINAL = ? AND COUNT = 0");

		updateCountEntries.setInt(1, countno);
		updateCountEntries.setInt(2, terminalno);

		updateCountEntries.executeUpdate();

		logger.log(Level.INFO, "Updated per-banknote records for BNA count for terminal " + terminalid);

		countInfo.close();
		createCountRecord.close();
		updateCountEntries.close();

		return countno;
	}


	public static BanknoteCount[] fetchBanknoteCount(String terminalid) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		logger.log(Level.INFO, "Starting to fetch banknote count in BNA in terminal " + terminalid);

		PreparedStatement ps = conn.prepareStatement("SELECT TERMINALID FROM TERMINAL WHERE HALLNAME= ?");

		ps.setString(1, terminalid);

		ResultSet rs = ps.executeQuery();

		int terminalno = 0;

		while (rs.next()) {
			terminalno = rs.getInt(1);
		}

		ArrayList<BanknoteCount> counts = new ArrayList<BanknoteCount>();

		PreparedStatement fetchBanknoteCounts = conn.prepareStatement("SELECT BANKNOTE, BANKNOTE_COUNT FROM COUNT_ENTRY WHERE TERMINAL = ? AND COUNT = 0");

		fetchBanknoteCounts.setInt(1, terminalno);

		ResultSet count = fetchBanknoteCounts.executeQuery();

		while (count.next()) {
			counts.add(new BanknoteCount(count.getInt(1), count.getInt(2)));
		}

		count.close();
		fetchBanknoteCounts.close();

		logger.log(Level.INFO, "Fetched " + counts.size() + " items in banknote count for terminal " + terminalid);

		return (counts.toArray(new BanknoteCount[]{}));
	}


	public static final int LLOG_TICKET_PURCHASE = 1;
	public static final int LLOG_TICKET_PLAY     = 2;
	public static final int LLOG_TICKET_END      = 3;
	public static final int LLOG_NO_TIKT_AVAIL   = 4;
	public static final int LLOG_TICKET_PAYOUT   = 5;


	public static int addLotteryLogRecord(int userid, String ticketno, int amount, int bet, int cellno, int win, int operday, int recordtype, int terminal) throws SQLException {

		logger.log(Level.INFO, "Adding record to lottery log: userid=" + userid + ", ticketno=" + ticketno + ", amount=" + amount + ", bet=" + bet + ", cellno=" + cellno + ", win=" + win +
				               ", operday=" + operday + ", recordtype=" + recordtype);

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("INSERT INTO LOTTERY_LOG(TICKETNO, AMOUNT, USERID, BET, CELLNO, WIN, OPERDAY, RECORDTYPE, TERMINAL) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
															Statement.RETURN_GENERATED_KEYS);

		if (ticketno == null) {
			ps.setString(1, "");
		} else {
			ps.setString(1, ticketno);
		}
		ps.setInt(2, amount);
		ps.setInt(3, userid);
		ps.setInt(4, bet);
		ps.setInt(5, cellno);
		ps.setInt(6, win);
		ps.setInt(7, operday);
		ps.setInt(8, recordtype);
		ps.setInt(9, terminal);

		int res = ps.executeUpdate();
		int recordno = 0;

		if (res == 1) {
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				recordno = rs.getInt(1);
			}
		}

		return recordno;
	}


	public static DoubleupRequestResult processPlayLongTicketDoubleupRequest(String terminal, String game, String pin, int bet, int win, String ticket, int playseq) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			String lottoProceedsAccnt = RequestProcessor.getLotteryProceedsAccountForGame(game);
			String lottoFundAccnt = RequestProcessor.getLotteryFundAccountForGame(game);

			String creditsOwnAccnt = RequestProcessor.getCreditsAccountForGame(terminal, game);
			String creditsWinAccnt = RequestProcessor.getWinCreditsAccountForGame(terminal, game);

			String bankOwnAccnt = RequestProcessor.getAccountForTerminal(terminal);
			String bankWinAccnt = RequestProcessor.getWinAccountForTerminal(terminal);

			String gameEscrowAccnt = RequestProcessor.getEscrowAccountForGameOnTerminal(terminal, game);

			int operday = RequestProcessor.getTerminalOperday(terminal);
			int escrowBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			if (bet > escrowBalance) {
				throw new SQLException("Double-up bet amount (" + bet + ") is more than win escrow balance (" + escrowBalance + ")");
			}

			TransactionProcessor.recordTransaction(gameEscrowAccnt, lottoProceedsAccnt, bet, TransactionProcessor.GAMEPLAY_TRANSACTION,
					"PIN: " + pin, 10, "Player bet on double-up", operday);

			TransactionProcessor.recordTransaction(lottoFundAccnt, gameEscrowAccnt, win, TransactionProcessor.GAMEWIN_TRANSACTION,
					"PIN: " + pin, 10, "Player deposit double-up win", operday);

			PreparedStatement ps = conn.prepareStatement("UPDATE PLAY_LOG SET DOUBLEUP = DOUBLEUP + 1 WHERE TICKET = ? AND PLAYSEQ = ?");
			ps.setString(1, ticket);
			ps.setInt(2, playseq);

			int res = ps.executeUpdate();

			if (res < 1) {
				logger.log(Level.SEVERE, "Can not update play log with double-up play");
				throw new SQLException("Can not update play log with double-up play");
			}

			long bankBalance = TransactionProcessor.getAccountBalance(bankOwnAccnt, bankWinAccnt);
			long creditsBalance = TransactionProcessor.getAccountBalance(creditsOwnAccnt, creditsWinAccnt);
			long winBalance = TransactionProcessor.getAccountBalance(gameEscrowAccnt);

			conn.commit();

			return new DoubleupRequestResult(true, bankBalance, creditsBalance, winBalance);
		} catch(Exception e) {
			try {
			conn.rollback();
			} catch (SQLException sqle) {
				logger.log(Level.SEVERE, "SQLException when rolling back double-up transaction", sqle);
			}
			logger.log(Level.SEVERE, "Exception caught while processing PlayLongTicket request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			}
			catch (SQLException e) {}
		}

		return new DoubleupRequestResult(false, 0L, 0L, 0L);
	}


	public static void createAccount(String accountno, int accountType, String accountOwner, int balance, String accountName) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("INSERT INTO ACCOUNT (ACCOUNTNO, ACCOUNTTYPE, OWNER, BALANCE, ACCOUNTNAME) VALUES (?, ?, ?, ?, ?)");

		ps.setString(1, accountno);
		ps.setInt(2, accountType);
		ps.setString(3, accountOwner);
		ps.setInt(4, balance);
		ps.setString(5, accountName);

		ps.executeUpdate();
	}


	public static boolean reconnectTerminal(String terminal) {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getNewConnection(false);

			PreparedStatement ps = conn.prepareStatement("SELECT STATE FROM TERMINAL WHERE HALLNAME = ?");
			ps.setString(1, terminal);

			ResultSet rs = ps.executeQuery();

			String state = null;

			if (rs.next()) {
				state = rs.getString(1);
				logger.log(Level.INFO, "Terminal state: " + state + " for terminal " + terminal);

				if (!state.equals("UNAVAILABLE")) {
					logger.log(Level.SEVERE, "Can not reconnect terminal " + terminal + "; terminal state = " + state);
					return false;
				} else {
					RequestProcessor.setTerminalState(terminal, "IDLE");
				}

			} else {
				logger.log(Level.SEVERE, "Can not find terminal in the database for terminal " + terminal);
				return false;
			}

			conn.commit();

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing init terminal request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (SQLException e) {}
		}

		return true;
	}



	public static synchronized TerminalRecord connectTerminal(String card, String ticket) {

		Connection conn = null;
		TerminalRecord terminalRecord = null;

		try {
			conn = ConnectionDispenser.getNewConnection(true);

			PreparedStatement ps = conn.prepareStatement("SELECT HALLNAME, ACTIVATED FROM TERMLIST WHERE CARDNO = ?");
			ps.setString(1, card);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				String terminal = rs.getString(1);
				boolean activated = rs.getBoolean(2);

				terminalRecord = RequestProcessor.initTerminal(card, activated);

				ps = conn.prepareStatement("SELECT STATE FROM TERMINAL WHERE TERMINALID = ?");
				ps.setInt(1, terminalRecord.getTerminalid());

				rs = ps.executeQuery();

				String state = null;

				if (rs.next()) {
					state = rs.getString(1);
					logger.log(Level.INFO, "Terminal state: " + state + " for terminal " + terminalRecord.getHallname() + " when connecting");

					// if terminal has been activated before, get the last ticket played; otherwise ignore the ticket supplied by terminal
					if (activated) {
						
						PreparedStatement ps2 = conn.prepareStatement("SELECT A.TOTALWIN, A.CASHEDOUT, IFNULL(A.PRINTED, FALSE) FROM TICKET_LOG A, PAPERTICKET B WHERE A.TICKET = B.TICKET AND B.PAPERTICKET = ?");
						ps2.setString(1, ticket);
	
						ResultSet rs2 = ps2.executeQuery();
	
						if (rs2.next()) {
							int totalwin = rs2.getInt(1);
							int timestamp = (int)rs2.getTimestamp(2).getTime();
							boolean printed = rs2.getBoolean(3);
	
							terminalRecord.setTicket(ticket);
							terminalRecord.setWin(totalwin);
							terminalRecord.setPrint(!printed);
							terminalRecord.setTimestamp(timestamp);
						} else {
							terminalRecord.setTicket("");
						}
					} else {
						terminalRecord.setTicket("");
					}
					
					if (!state.equals("UNAVAILABLE")) {
						return null;
					} else {
						// Fix for double-terminal card initialization bug; 11/22/2009
						RequestProcessor.setTerminalState(terminal, "IDLE");
					}

				} else {
					logger.log(Level.SEVERE, "Can not find terminal in the database for terminal name " + terminal);
					return null;
				}
			} else {
				logger.log(Level.SEVERE, "Can not find terminal in initial terminal init list for card: " + card);
				return null;
			}

		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while processing init terminal request", e);
		} finally {
			try {
				conn.close();
				ConnectionDispenser.releaseConnection();
			} catch (SQLException e) {}
		}

		return terminalRecord;
	}

	private static TerminalRecord populateTerminalRecord(ResultSet rs) throws SQLException {

		String cardno      = rs.getString(1);
		String serialno    = rs.getString(2);
		String assetid     = rs.getString(3);
		String hallname    = rs.getString(4);
		String description = rs.getString(5);
		boolean activated  = rs.getBoolean(6);
		String gameset     = rs.getString(7);
		int terminalid     = rs.getInt(8);

		TerminalRecord terminalRecord = new TerminalRecord();

		terminalRecord.setCardno(cardno);
		terminalRecord.setSerialno(serialno);
		terminalRecord.setAssetid(assetid);
		terminalRecord.setHallname(hallname);
		terminalRecord.setDescription(description);
		terminalRecord.setActivated(activated);
		terminalRecord.setGameset(gameset);
		terminalRecord.setTerminalid(terminalid);

		return terminalRecord;
	}


	private static int storeTerminalInfo(TerminalRecord terminalRecord) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();
		
		// Fix for defect GRUKR-1091: Newly installed terminal can be activated during daily count mode
		PreparedStatement ps = conn.prepareStatement("SELECT OPERDAY, COUNTTYPE FROM CASH_REGISTER WHERE REGISTERTYPE = 'MAIN'");

		ResultSet rs = ps.executeQuery();
		int operday = -1;

		if (rs.next()) {
			operday = rs.getInt(1);
			String counttype = rs.getString(2);
			
			if (counttype.equals("DAILY")) {
				operday = operday + 1;
			}
			
		} else {
			logger.log(Level.SEVERE, "Can not get current operday from MAIN cash register");
			throw new SQLException("Can not get current operday from MAIN cash register");
		}

		ps = conn.prepareStatement("INSERT INTO TERMINAL (TERMINALID, SERIALNO, ASSETID, HALLNAME, ACCOUNT, DESCRIPTION, OPERDAY, PINACCOUNT, WINACCOUNT, STATE, DENOMINATION) VALUES " +
                                                               "(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, 20) ON DUPLICATE KEY UPDATE STATE='UNAVAILABLE'", Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, terminalRecord.getSerialno());
		ps.setString(2, terminalRecord.getAssetid());
		ps.setString(3, terminalRecord.getHallname());
		ps.setString(4, "TERMINAL.BNA.CASH." + terminalRecord.getHallname());
		ps.setString(5, terminalRecord.getDescription());
		ps.setInt(6,    operday);
		ps.setString(7, "TERMACCNT." + terminalRecord.getHallname());
		ps.setString(8, "TERMACCNT." + terminalRecord.getHallname() + ".WIN");
		ps.setString(9, "UNAVAILABLE");

		ps.executeUpdate();

		rs = ps.getGeneratedKeys();

		if (rs.next()) {
			return rs.getInt(1);
		} else {
			return -1;
		}
	}


	private static void createTerminalAccounts(String terminal) throws SQLException {

		try {
			RequestProcessor.createAccount("TERMACCNT." + terminal,          1, "TERMACCNT." + terminal,          0, "TERMACCNT." + terminal);
			RequestProcessor.createAccount("TERMACCNT." + terminal + ".WIN", 1, "TERMACCNT." + terminal + ".WIN", 0, "TERMACCNT." + terminal + ".WIN");
			RequestProcessor.createAccount("TERMINAL.BNA." + terminal,       1, "TERMINAL.BNA." + terminal,       0, "TERMINAL.BNA." + terminal);
			RequestProcessor.createAccount("TERMINAL.BNA.CASH." + terminal,  1, "TERMINAL.BNA.CASH." + terminal,  0, "TERMINAL.BNA.CASH." + terminal);
			RequestProcessor.createAccount("TERM.EMERGENCY.COUNT." + terminal, 1, "TERM.EMERGENCY.COUNT." + terminal, 0, "TERM.EMERGENCY.COUNT." + terminal);
		} catch(SQLIntegrityConstraintViolationException e) {
			logger.log(Level.INFO, "Accounts for terminal " + terminal + " already exist in the system");
		}
	}


	private static void createGameAccounts(String game, String terminal) throws SQLException {
		try {
			RequestProcessor.createAccount("GAMEESCROW.TERM." + terminal + ".GAME." + game, 1, "GAMEESCROW.TERM." + terminal + ".GAME." + game, 0, "GAMEESCROW.TERM." + terminal + ".GAME." + game);
			RequestProcessor.createAccount("CREDITS.GAME.TERM." + terminal + ".GAME." + game, 1, "CREDITS.GAME.TERM." + terminal + ".GAME." + game, 0, "CREDITS.GAME.TERM." + terminal + ".GAME." + game);
			RequestProcessor.createAccount("CREDITS.GAME.TERM." + terminal + ".GAME." + game + ".WIN", 1, "CREDITS.GAME.TERM." + terminal + ".GAME." + game + ".WIN", 0, "CREDITS.GAME.TERM." + terminal + ".GAME." + game + ".WIN");
		} catch(SQLIntegrityConstraintViolationException e) {
			logger.log(Level.INFO, "Accounts for terminal " + terminal + " and game " + game + " already exist in the system");
		}
	}


	private static void updateTerminalId(TerminalRecord terminalRecord, int terminalid) throws SQLException {

		terminalRecord.setTerminalid(terminalid);

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("UPDATE TERMLIST SET TERMINALID = ? WHERE CARDNO = ?");

		ps.setInt(1, terminalid);
		ps.setString(2, terminalRecord.getCardno());

		ps.executeUpdate();

		terminalRecord.setTerminalid(terminalid);
	}


	private static void processGameset(String gameset, String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT GAMENAME FROM GAMESET WHERE GAMESET = ?");
		ps.setString(1, gameset);

		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String game = rs.getString(1);

			logger.log(Level.INFO, "Creating game account for game " + game + " on terminal " + terminal);

			createGameAccounts(game, terminal);
		}

		rs.close();
		ps.close();
	}


	public static TerminalRecord initTerminal(String card, boolean activated) throws SQLException {

		TerminalRecord terminalRecord = null;

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT CARDNO, SERIALNO, ASSETID, HALLNAME, DESCRIPTION, ACTIVATED, GAMESET, TERMINALID FROM TERMLIST WHERE CARDNO = ?");

		ps.setString(1, card);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {

			terminalRecord = populateTerminalRecord(rs);

			if (!terminalRecord.isActivated())
			{
				PreparedStatement ps1 = conn.prepareStatement("INSERT INTO INSTALLED_TERMINALS(SERIALNUMBER) VALUES(?)");
				ps1.setString(1, terminalRecord.getHallname());

				ps1.executeUpdate();

				logger.log(Level.INFO, "Added newly installed terminal " + terminalRecord.getHallname() + " for post-processing");

				int terminalid = storeTerminalInfo(terminalRecord);

				if (terminalid != -1) {
					updateTerminalId(terminalRecord, terminalid);
				} else {
					logger.log(Level.SEVERE, "Can not store terminal info for terminal " + terminalRecord.getHallname());
					throw new SQLException("Can not store terminal info for terminal " + terminalRecord.getHallname());
				}

				createTerminalAccounts(terminalRecord.getHallname());
				// processGameset(terminalRecord.getGameset(), terminalRecord.getHallname());
				processGameset("NOV1", terminalRecord.getHallname());
				processGameset("NOV2", terminalRecord.getHallname());
				processGameset("NOV3", terminalRecord.getHallname());
				processGameset("NOV4", terminalRecord.getHallname());
				processGameset("IGR1", terminalRecord.getHallname());
				processGameset("ATR1", terminalRecord.getHallname());


				ps = conn.prepareStatement("UPDATE TERMLIST SET ACTIVATED = TRUE WHERE TERMINALID = ?");

				ps.setInt(1, terminalid);

				ps.executeUpdate();

			} else {
				logger.log(Level.INFO, "Terminal " + terminalRecord.getHallname() + " (cardno : " + card + ") already activated");
			}

			PreparedStatement ps2 = conn.prepareStatement("SELECT DENOMINATION FROM TERMINAL WHERE TERMINALID = ?");
			ps2.setInt(1, terminalRecord.getTerminalid());

			ResultSet rs2 = ps2.executeQuery();

			if (rs2.next()) {
				int denomination = rs2.getInt(1);
				terminalRecord.setDenomination(denomination);
			}
		} else {
			// terminal info not found
			logger.log(Level.SEVERE, "Terminal not found");
			throw new SQLException("Terminal info not found ( cardno : " + card + " )");
		}

		return terminalRecord;
	}


	public static Denomination getTerminalDenomination(String terminal) {
		Connection conn = null;
		Denomination denomination = null;

		try {
			conn = ConnectionDispenser.getNewConnection(true);
			denomination = RequestProcessor.getDenomination(terminal);
		} catch(SQLException e) {
			logger.log(Level.SEVERE, "Exception caught while retrieving terminal denomination", e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {}

			ConnectionDispenser.releaseConnection();
		}

		return denomination;
	}
}
