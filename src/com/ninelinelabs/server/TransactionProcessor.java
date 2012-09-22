/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TransactionProcessor.java 139 2011-06-04 05:04:55Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 * Jun 03, 2011 mloukianov Added check for account balance for accounts tied to credits and currency on terminal and game
 */
package com.ninelinelabs.server;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ninelinelabs.server.cashless.AccountNotFoundException;
import com.ninelinelabs.server.cashless.TransactionException;
import com.ninelinelabs.util.database.ConnectionDispenser;

public class TransactionProcessor {

	public static final int GAMEPLAY_TRANSACTION = 1;				// Game play transaction - place a bet
	public static final int GAMEWIN_TRANSACTION = 2;				// Game win transaction - receive winnings
	public static final int WINDEPOSIT_TRANSACTION = 3;				// Win deposit transaction
	public static final int TERMINALLOAD_TRANSACTION = 4;
	public static final int TERMINALBNACASHLOAD_TRANSACTION = 5;
	public static final int CASHOUT_TRANSACTION = 6;
	public static final int CREDITSTOBANK_TRANSACTION = 7;
	public static final int BANKTOCREDITS_TRANSACTION = 8;
	public static final int BNACOUNT_TRANSACTION = 9;

	private final static Logger logger = Logger.getLogger(TransactionProcessor.class.getName());

	/**
	 * Used to add record to transaction log in the database.
	 *
	 * @param conn       connection to use
	 * @param from       debit account number
	 * @param to         credit account number
	 * @param amount     transaction amount
	 * @param type       transaction type
	 * @param user       user who initiated the transaction
	 * @param location   location where transaction was initiated
	 * @param parentTransaction  parent transaction id
	 * @param memo       transaction memo
	 * @param operday    operday for this transaction
	 *
	 * @return transaction id; transaction id must be used as a foreign key for transaction detail lookup
	 *
	 * @throws LoggingException
	 */
	private static int addTransactionLogRecord(String from, String to, int amount, int type, String user, int location, int parentTransaction, String memo, int operday)
			throws LoggingException {

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = ConnectionDispenser.getConnection();

			// Insert transaction information into the main transaction log
			ps = conn.prepareStatement("INSERT INTO TRANSACTION_LOG(TXNO, TXTS, DEBIT, CREDIT, AMOUNT, TXTYPE, USERNAME, LOCATION, PARENT_TXNO, OPERDAY) VALUES (NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

			ps.setString(1, from);
			ps.setString(2, to);
			ps.setInt(   3, amount);
			ps.setInt(   4, type);
			ps.setString(5, user);
			ps.setInt(   6, location);
			ps.setInt(   7, parentTransaction);
			ps.setInt(   8, operday);

			if (ps.executeUpdate() == 0) {
				// TODO: probably should never happen unless something is wrong with the database
				logger.log(Level.SEVERE, "Can not insert transaction log record; performing transaction rollback");
				conn.rollback();
				throw new LoggingException(LoggingException.TRANSACTION_LOG);
			}

			// get last transaction id for this connection
			/*
			 * From javadoc for java.sql.Statement getGeneratedKeys()
			 * Retrieves any auto-generated keys created as a result of executing this Statement object.
			 * If this Statement object did not generate any keys, an empty ResultSet object is returned.
			 */
			rs = ps.getGeneratedKeys();

			if (rs.next()) {
				return rs.getInt(1);
			} else {
				conn.rollback();
				throw new LoggingException(LoggingException.TRANSACTION_LOG);
			}
		} catch(SQLException sqle) {
			throw new LoggingException(LoggingException.TRANSACTION_LOG, sqle);
		} finally {
			try {
				rs.close();
				ps.close();
			} catch (SQLException sqle) {
				// Ignore
			}
		}
	}

	/**
	 * Record transaction in transaction log in the database.
	 *
	 * @param conn               database connection
	 * @param from               debit account
	 * @param to                 credit account
	 * @param amount             transaction monetary amount
	 * @param type               transaction type
	 * @param user               user who performed the transaction
	 * @param location           location where the transaction was initiated
	 * @param parentTransaction  parent transaction id
	 *
	 * @return  transaction id (integer)
	 *
	 * @throws TransactionException  when transaction can not be recorded
	 * @throws AccountNotFoundException
	 */
	static int recordTransaction(String from, String to, int amount, int type, String user, int location, int parentTransaction, String memo, int operday)
			throws TransactionException, AccountNotFoundException {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getConnection();

			/*
			 * the following prefixes determine the account that must be non-negative:
			 * CREDITS.GAME.TERM  *
			 * GAMEESCROW.TERM  *
			 * LOTTO.PROCEEDS
			 * PIN. - not always; only for player accounts
			 * TERMACCNT.  *
			 *
			 * the following prefixes determine that the account must be non-positive
			 * LOTTO.FUND
			 * PLAYER.CASH.PIN. - not sure about this one
			 * TERMINAL.BNA
			 */

			if (from.startsWith("CREDITS.GAME.TERM") || from.startsWith("GAMEESCROW.TERM.") || from.startsWith("TERMACCNT")) {

				if (getAccountBalance(from) < amount) {
					throw new TransactionException("Account " + from + " balance (" + getAccountBalance(from) + ") is less then transaction amount (" + amount + ")");
				}
			}

			PreparedStatement ps = conn.prepareStatement("UPDATE ACCOUNT SET BALANCE = (BALANCE - ?) WHERE ACCOUNTNO = ?");
			ps.setInt(1, amount);
			ps.setString(2, from);

			if (ps.executeUpdate() < 1) {
				logger.log(Level.SEVERE, "Debiting account " + from + " in the amount of " + amount + " failed");
				conn.rollback();
				throw new TransactionException(TransactionException.ACCOUNT_NOT_FOUND + from);
			}

			ps.close();

			ps = conn.prepareStatement("UPDATE ACCOUNT SET BALANCE = (BALANCE + ?) WHERE ACCOUNTNO = ?");
			ps.setInt(1, amount);
			ps.setString(2, to);

			if (ps.executeUpdate() < 1) {
				logger.log(Level.SEVERE, "Crediting account " + to + " in the amount of " + amount + " failed");
				conn.rollback();
				throw new TransactionException(TransactionException.ACCOUNT_NOT_FOUND + to);
			}

			ps.close();

			return addTransactionLogRecord(from, to, amount, type, user, location, parentTransaction, memo, operday);

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception trying to record financial transaction", e);
			throw new TransactionException(e);
		} catch (LoggingException e) {
			logger.log(Level.SEVERE, "Exception trying to record financial transaction", e);
			throw new TransactionException(e);
		}
	}

	/**
	 * Record transaction in transaction log in the database.
	 *
	 * @param conn      database connection
	 * @param from      debit account
	 * @param to        credit account
	 * @param amount    transaction monetary amount
	 * @param type      transaction type
	 * @param user      user who performs the transaction
	 * @param location  location where the transaction was initiated
	 *
	 * @return  transaction id (integer)
	 *
	 * @throws TransactionException  when transaction can not be recorded
	 * @throws AccountNotFoundException
	 */
	public static int recordTransaction(String from, String to, int amount, int type, String user, int location, String memo, int operday)
			throws TransactionException, AccountNotFoundException {

		return recordTransaction(from, to, amount, type, user, location, -1, memo, operday);
	}


	/**
	 * Record coupled transactions in transaction log in the database.
	 *
	 * @param conn      database connection
	 * @param fromCash  debit cash account
	 * @param toCash    credit cash account
	 * @param from      debit account
	 * @param to        credit account
	 * @param amount    transaction monetary amount
	 * @param type      transaction type
	 * @param user      user who performs the transaction
	 * @param location  location where the transaction was initiated
	 *
	 * @return  transaction id (integer)
	 *
	 * @throws TransactionException  when transaction can not be recorded
	 * @throws AccountNotFoundException
	 */
	static int recordCashTransaction(String fromCash, String toCash, String from, String to, int amount, int type, String user, int location, String memo, int operday)
			throws TransactionException, AccountNotFoundException {

		int tx = recordTransaction(from, to, amount, type, user, location, memo, operday);
		return recordTransaction(fromCash, toCash, amount, type, user, location, tx, memo, operday);
	}


	public static int recordConversion(String fromAccnt, String fromConversionAccnt, String toConversionAccnt, String toAccnt, int amount, int exchangeRate, int type, String user, int location, String memo, int operday)
			throws TransactionException, AccountNotFoundException {

		int tx = recordTransaction(fromAccnt, fromConversionAccnt, amount, type, user, location, memo, operday);

		int exchangedAmount = amount / exchangeRate;
		return recordTransaction(toConversionAccnt, toAccnt, exchangedAmount, type, user, location, tx, memo, operday);
	}

	public static int recordBackConversion(String fromAccnt, String fromConversionAccnt, String toConversionAccnt, String toAccnt, int amount, int exchangeRate, int type, String user, int location, String memo, int operday)
			throws TransactionException, AccountNotFoundException {

		// NOTE: transaction order changed to avoid potential deadlock
		int exchangedAmount = amount * exchangeRate;
		int tx = recordTransaction(toConversionAccnt, toAccnt, exchangedAmount, type, user, location, memo, operday);

		return recordTransaction(fromAccnt, fromConversionAccnt, amount, type, user, location, tx, memo, operday);
	}

	/**
	 * Returns up-to-date account balance
	 *
	 * @param conn    database connection
	 * @param accnt   first account number
	 *
	 * @return up-to-date account balance
	 *
	 * @throws AccountNotFoundException
	 */
	public static int getAccountBalance(String accnt) throws AccountNotFoundException {

		Connection conn = null;

		try {
			conn = ConnectionDispenser.getConnection();

			PreparedStatement ps = conn.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ACCOUNTNO = ?");
			ps.setString(1, accnt);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				int balance = rs.getInt(1);
				return balance;
			} else {
				logger.log(Level.SEVERE, "Account " + accnt + " not found");
				throw new AccountNotFoundException("Account " + accnt + " not found");
			}
		} catch(SQLException sqle) {
			throw new AccountNotFoundException(sqle);
		}
	}

	/**
	 * Returns account balance for two accounts combined
	 * (used in case where we track own funds and winning funds separately)
	 *
	 * @param conn    database connection
	 * @param account   first account number
	 * @param winaccount   second account number
	 */
	public static int getAccountBalance(String account, String winaccount) throws AccountNotFoundException {
		Connection conn = null;

		try {
			conn = ConnectionDispenser.getConnection();

			PreparedStatement ps = conn.prepareStatement("SELECT (A.BALANCE + B.BALANCE) BALANCE FROM ACCOUNT A, ACCOUNT B WHERE A.ACCOUNTNO = ? AND B.ACCOUNTNO = ?");

			ps.setString(1, account);
			ps.setString(2, winaccount);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			} else {
				logger.log(Level.SEVERE, "No account found for account no: " + account + "or for account no: " + winaccount);
				throw new AccountNotFoundException("Account " + account + " or account " + winaccount + " not found");
			}
		} catch(SQLException sqle) {
			throw new AccountNotFoundException(sqle);
		}
	}
}
