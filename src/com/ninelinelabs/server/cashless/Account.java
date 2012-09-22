/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Account.java $
 *
 * Date Author Changes
 * Feb 03, 2009 mloukianov Created
 * Mar 25, 2009 mloukianov Added some stuff for temp and roll-up account support
 *
 */
package com.ninelinelabs.server.cashless;

import java.sql.Connection;
import java.sql.SQLException;

 /**
  * A class representing account in the system.
  * Account can be either cashless account, roll-up account, or temporary account.
  *
  * Cashless account has to be stored in the database at all times, and generally
  * assigned to this session, i.e. can not be locked by any other thread.
  *
  * Roll-up account is a general account that is used by different threads
  * independently, but it can be rolled-up at the end of player session.
  *
  * Temporary accounts are created for the duration of the session, and must
  * have their balances cleaned out when session closes.
  *
  * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
  * @version $Revision: 112 $ $Date: 2011-05-28 02:22:03 -0500 (Sat, 28 May 2011) $
  * @see
  */
 public abstract class Account {

	public static final int PERMANENT_CASH = 1;
	public static final int PERMANENT_NONCASH = 2;
	public static final int TRANSIENT_CASH = 3;
	public static final int TRANSIENT_NONCASH = 4;

	public enum AccountType {
		CASHLESS, ROLLUP, TEMPORARY
	}


	private final String accountno;
	private int accountid;
	protected boolean consolidated = false;


	/**
	 * Creates account object; account number and account type are final
	 * and can not be changed later.
	 *
	 * @param accountno   Account number
	 */
	public Account(String accountno) {
		this.accountno = accountno;
	}

	/**
	 * Sets account id. Account id is used internally.
	 *
	 * @param accountid   Account id
	 */
	public void setAccountId(int accountid) {
		this.accountid = accountid;
	}

	/**
	 * Returns current account balance
	 *
	 * @return  Current account balance
	 */
	abstract public int getBalance(Connection conn) throws SQLException;

	/**
	 * Returns account id. Account id does not match account number and for internal use only
	 *
	 * @return  Account id
	 */
	public int getAccountId() {
		return this.accountid;
	}

	/**
	 * Returns account number.
	 *
	 * @return   Account number
	 */
	public String getAccountNo() {
		return this.accountno;
	}

	/**
	 * Returns account type.
	 *
	 * @return   Account type
	 */
	abstract public AccountType getAccountType();

	abstract public void debitAccount(Connection conn, int amount) throws SQLException;

	abstract public void creditAccount(Connection conn, int amount) throws SQLException;

	abstract public void consolidate(Connection conn) throws SQLException, AccountConsolidationException;
}
