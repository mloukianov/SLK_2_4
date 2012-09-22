/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: CashlessService.java $
 *
 * Date Author Changes
 * Feb 03, 2009 mloukianov Created
 * 
 */
package com.ninelinelabs.server.cashless;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An class representing cashless service implementation
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 * @see 
 */
public class CashlessService {
	private final static Logger logger = Logger.getLogger(CashlessService.class.getName());
	
	
	/**
	 * Creates temporary account with zero balance.
	 * Transactional semantics: commits a transaction.
	 * 
	 * @param conn      database connection to be used
	 * @param account   account number
	 * @param type      account type
	 * @param owner     account owner
	 * @param comment   account comment
	 */
	public void createTemporaryAccount(Connection conn, String account, int type, String owner, String comment) 
																		throws TemporaryAccountExistsException, SQLException {
		PreparedStatement ps = null;
		
		try {
			conn.setAutoCommit(false);
			
			ps = conn.prepareStatement("SELECT ACCOUNTNO FROM ACCOUNT WHERE ACCOUNTNO = ?");
			ps.setString(1, account);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				rs.close();
				throw new TemporaryAccountExistsException(account);
			}
				
			ps = conn.prepareStatement("INSERT INTO ACCOUNT VALUES (?, ?, ?, 0, ?)");
				
			ps.setString(1, account);
			ps.setInt(2, type);
			ps.setString(3, owner);
			ps.setString(4, comment);
				
			ps.execute();
				
			conn.commit();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "SQLException caught while creating temporary account", e);
			throw e;
		}
	}
	
	/**
	 * Removes temporary account after checking it has zero balance.
	 * Transactional semantics: commits a transaction.
	 * 
	 * @param conn      database connection to be used
	 * @param account   account number
	 * 
	 * @throws TemporaryAccountNonzeroBalanceException  when temporary account has non-zero balance
	 * @throws TemporaryAccountNotFoundException        when temporary account not found
	 * @throws SQLException                             any other dtaabase problems
	 */
	public void removeTemporaryAccount(Connection conn, String account) 
			throws TemporaryAccountNonzeroBalanceException, TemporaryAccountNotFoundException, SQLException {
		
		PreparedStatement ps = null;
		
		try {
			conn.setAutoCommit(false);
			
			ps = conn.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ACCOUNTNO = ?");
			ps.setString(1, account);
			
			ResultSet rs = ps.executeQuery();
		
			// TODO: ACCOUNT table must have unique constraint on ACCOUNTNO column - CHECK!
			if (rs.next()) {
				int balance = rs.getInt(1);
				if (balance != 0) {
					rs.close();
					throw new TemporaryAccountNonzeroBalanceException(account);
				}
			} else {
				throw new TemporaryAccountNotFoundException(account);
			}
			rs.close();
				
			ps = conn.prepareStatement("DELETE FROM ACCOUNT WHERE ACCOUNTNO = ?");
				
			ps.setString(1, account);
				
			ps.execute();
				
			conn.commit();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "SQLException caught while removing temporary account", e);
			throw e;
		}
	}
}
