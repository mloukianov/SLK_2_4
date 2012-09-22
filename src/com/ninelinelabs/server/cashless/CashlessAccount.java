/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: CashlessAccount.java $
 *
 * Date Author Changes
 * Mar 30, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.cashless;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class representing
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 * @see
 */
public class CashlessAccount extends Account {

	/**
	 * @param accountno
	 */
	public CashlessAccount(String accountno) {
		super(accountno);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#consolidate(java.sql.Connection)
	 */
	@Override
	public void consolidate(Connection conn) throws SQLException,
			AccountConsolidationException {
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#creditAccount(java.sql.Connection, int)
	 */
	@Override
	public void creditAccount(Connection conn, int amount) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("UPDATE ACCOUNT SET BALANCE = (BALANCE + ?) WHERE ACCOUNTNO = ?");

		ps.setInt(1, amount);
		ps.setString(2, this.getAccountNo());

		int result = ps.executeUpdate();

		if (result != 1) {
			throw new SQLException("Account not found: " + this.getAccountNo());
		}
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#debitAccount(java.sql.Connection, int)
	 */
	@Override
	public void debitAccount(Connection conn, int amount) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("UPDATE ACCOUNT SET BALANCE = (BALANCE - ?) WHERE ACCOUNTNO = ?");

		ps.setInt(1, amount);
		ps.setString(2, this.getAccountNo());

		int result = ps.executeUpdate();

		if (result != 1) {
			throw new SQLException("Account not found: " + this.getAccountNo());
		}
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#getAccountType()
	 */
	@Override
	public AccountType getAccountType() {
		// TODO Auto-generated method stub
		return AccountType.CASHLESS;
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#getBalance(java.sql.Connection)
	 */
	@Override
	public int getBalance(Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ACCOUNTNO = ?");

		ps.setString(1, this.getAccountNo());

		ResultSet rs = ps.executeQuery();

		int balance = 0;

		if (rs.next()) {
			balance = rs.getInt(1);
		} else {
			throw new SQLException("Account not found: " + this.getAccountNo());
		}

		return balance;
	}
}
