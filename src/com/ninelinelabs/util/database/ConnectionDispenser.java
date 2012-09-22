/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ConnectionDispenser.java 130 2011-05-28 08:36:51Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * March 07, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.util.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Maintains single connection per thread using ThreadLocal.
 *
 * @author mloukianov
 */
public class ConnectionDispenser {

	private static DataSource ds;

	/**
	 * ThreadLocal class to store database connection used by this thread
	 *
	 * @author mloukianov
	 */
	private static class ThreadLocalConnection extends ThreadLocal<Connection> {

		public Connection initialValue() {
			synchronized(ds) {
				try {
					return ds.getConnection();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static ThreadLocalConnection conn = new ThreadLocalConnection();


	/**
	 * Get connection from this method; if connection is not available, initialValue() will be called
	 *
	 * @return  database connection from datasource
	 */
	public static Connection getConnection() {

		return conn.get();
	}


	public static Connection getNewConnection(boolean autocommit) throws SQLException {

		Connection connection = null;

		synchronized(ds) {
			connection = ds.getConnection();
		}

		connection.setAutoCommit(autocommit);
		conn.set(connection);

		return connection;
	}


	public static void releaseConnection() {
		conn.set(null);
	}


	/**
	 * Set datasource to use to get a connection from
	 *
	 * @param dataSource
	 */
	public static void setDataSource(DataSource dataSource) throws NullPointerException {
		if (dataSource == null)
			throw new NullPointerException("DataSource parameter is null");

		ds = dataSource;
	}
}
