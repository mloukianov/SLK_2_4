/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: RollupAccount.java 118 2011-05-28 07:28:25Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 30, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.cashless;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A class representing oll-up account.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 118 $ $Date: 2011-05-28 02:28:25 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class RollupAccount extends Account {

	private int delta;

	public RollupAccount(String accountno) {
		super(accountno);
		this.delta = 0;
	}


	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#consolidate(java.sql.Connection)
	 */
	@Override
	public void consolidate(Connection conn) throws SQLException,
			AccountConsolidationException {

		PreparedStatement ps = conn.prepareStatement("UPDATE ACCOUNT SET BALANCE = (BALANCE + ?) WHERE ACCOUNTNO = ?");

		ps.setInt(1, this.delta);
		ps.setString(2, this.getAccountNo());

		int result = ps.executeUpdate();

		if (result != 1) {
			throw new AccountConsolidationException("Could not find account to be consolidated; account no : " + this.getAccountNo());
		}
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#creditAccount(java.sql.Connection, int)
	 */
	@Override
	public void creditAccount(Connection conn, int amount) throws SQLException {
		this.delta = this.delta + amount;
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#debitAccount(java.sql.Connection, int)
	 */
	@Override
	public void debitAccount(Connection conn, int amount) throws SQLException {
		this.delta = this.delta - amount;
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#getAccountType()
	 */
	@Override
	public AccountType getAccountType() {
		return AccountType.ROLLUP;
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.cashless.Account#getBalance(java.sql.Connection)
	 */
	@Override
	public int getBalance(Connection conn) throws SQLException {
		// TODO Auto-generated method stub
		return delta;
	}
}
