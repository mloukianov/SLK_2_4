/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: MySQLDAOFactory.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 7, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.lottery.dao.mysql;

import java.sql.Connection;

import com.ninelinelabs.server.lottery.dao.DAOFactory;
import com.ninelinelabs.server.lottery.dao.LotteryBatchDAO;

/**
 * A concrete implementation of DAO factory for MySQL database.
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
public class MySQLDAOFactory extends DAOFactory {

	public MySQLDAOFactory() {
	}

	/* (non-Javadoc)
	 * @see com.ninelinelabs.server.lottery.dao.DAOFactory#getLotteryBatchDAO()
	 */
	@Override
	public LotteryBatchDAO getLotteryBatchDAO(Connection conn) {
		return new MySQLLotteryBatchDAO(conn);
	}

}
