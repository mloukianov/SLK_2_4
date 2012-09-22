/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TerminalRegistry.java 104 2011-05-28 07:17:51Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;


public class TerminalRegistry {

	private static final Logger logger = Logger.getLogger(GameRegistry.class.getName());

	public static final TerminalRegistry INSTANCE = new TerminalRegistry();

	@SuppressWarnings("unused")
	private DataSource ds;

	private TerminalRegistry() {

		Context ctx = null;

		try {
			final StandardServer server = (StandardServer)ServerFactory.getServer();
			ctx = server.getGlobalNamingContext();

			if (ctx != null) {
				ds = (DataSource)ctx.lookup("jdbc/GameServerDB");
			} else {
				ds = null;
			}
		} catch (NamingException ne) {
			logger.log(Level.SEVERE, "Can not get DataSource; NamingException caught", ne);
		} finally {
		}
	}

/*
	public void init() {

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			if (ds != null) {
				synchronized(ds) {
					conn = ds.getConnection();
				}

				ps = conn.prepareStatement("SELECT TERMINALID, SERIALNO, ASSETID, HALLNAME, ACCOUNT, DESCRIPTION, OPERDAY, PINACCOUNT, WINACCOUNT, STATE, CARDNO FROM TERMINAL WHERE CARDNO IS NOT NULL");

				rs = ps.executeQuery();

				while (rs.next()) {
					String gameid = rs.getString(1);
					String gametype = rs.getString(2);
					String name = rs.getString(3);
					int denom = rs.getInt(4);

					logger.log(Level.INFO, "Adding game: [" + gameid + ", " + gametype + ", " + name + ", " + denom + "]");

					GameService gameService = new GameServiceImpl(gameid, gametype, name, denom);

					String lotteryid = rs.getString(5);
					int lotteryprice = rs.getInt(6);

					if (lotteryid != null) {
						LotteryService lotteryService = null;
						if (gametype.equals("LONGTICKET")) {
							lotteryService = new LongTicketLotteryServiceImpl(lotteryid, lotteryprice);
						} else if (gametype.equals("SHORTTICKET")) {
							lotteryService = new LotteryServiceImpl(lotteryid, lotteryprice);
						}
						gameService.setRandomSource(lotteryService);
					}

					services.put(gameid, gameService);
				}

			} else {
				logger.log(Level.SEVERE, "Cannot load game metadata: DataSource not available");
			}
		} catch (SQLException sqle) {
			logger.log(Level.SEVERE, "Exception during GameRegistry initialization", sqle);
		} finally {
			try {
				rs.close();
			} catch (SQLException sqle) {
				// Ignore
			}
			try {
				ps.close();
			} catch (SQLException sqle) {
				// Ignore
			}
		}
	}*/
}
