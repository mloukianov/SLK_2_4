/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: DAOFactory.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 7, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.lottery.dao;

import java.sql.Connection;

import com.ninelinelabs.server.lottery.dao.mysql.MySQLDAOFactory;

/**
 * Abstract DAO factory for lottery batches.
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public abstract class DAOFactory {
	public static final int MYSQL = 1;
	public static final int ORACLE = 2;
	public static final int POSTGRESQL = 3;
	public static final int CLOUDSCAPE = 4;

	public abstract LotteryBatchDAO getLotteryBatchDAO(Connection conn);

	public static DAOFactory getDAOFactory(int factory) {
		switch(factory) {
		case MYSQL:
			return new MySQLDAOFactory();
		case ORACLE:
			return null;
		case POSTGRESQL:
			return null;
		case CLOUDSCAPE:
			return null;
		}
		return null;
	}
}
