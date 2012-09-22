/*
 * Copyright (C) 2008-2012, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: GameRegistry.java $
 *
 * Date Author Changes
 * Mar 18, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;

import com.ninelinelabs.protocol.hpsp.Protocol;
import com.ninelinelabs.services.GameService;
import com.ninelinelabs.services.GameServiceImpl;
import com.ninelinelabs.services.LotteryService;
import com.ninelinelabs.services.LotteryServiceImpl;
import com.ninelinelabs.services.LongTicketLotteryServiceImpl;

/**
 * A singleton class used to load game definitions and provide access to game
 * object model.  Game object model is read-only for the session mechanism.
 *
 * For example:
 * <pre>
 *   GameRegistry registry = GameRegistry.getInstance();
 *   Game game = registry.get("GBOR0122");
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 */
public class GameRegistry {

	private static final Logger logger = Logger.getLogger(GameRegistry.class.getName());

	public static final GameRegistry INSTANCE = new GameRegistry();

	private DataSource ds;

	private final HashMap<String, GameService> services;
	private final HashMap<String, String> games;
	private final HashMap<String, String> lotteries;



	public static GameRegistry getInstance() {
		return INSTANCE;
	}

	private void initializeLotteries() {

		lotteries.put("GBOR0122", "LOT_BOR_NOBONUS");
		lotteries.put("GLLC0500", "LOT_LLC_NOBONUS");
		lotteries.put("GDPL0100", "LOT_LLC_NOBONUS");
		lotteries.put("GBORSLOT", "LOT_BOR_NOBONUS");
		lotteries.put("GBOR0123", "LOT_BOR_NOBONUS");
		lotteries.put("GLLCSLOT", "LOT_LLC_NOBONUS");
		lotteries.put("GDPLSLOT", "LOT_LLC_NOBONUS");
		lotteries.put("GMPL0500", "LOT_MPL_NOBONUS");
		lotteries.put("GSHL0500", "LOT_SHL_NOBONUS");
		lotteries.put("GHCL0500", "LOT_HCL_NOBONUS");

		// Novolot-1

		lotteries.put("GSHL0500B", "LOT_SHL_BONUS");	// Sizzling Hot	GSHL0500B
		lotteries.put("GMPL0500B", "LOT_MPL_BONUS");	// Marco Polo	GMPL0500B
		lotteries.put("GLLC0500B", "LOT_LLC_BONUS");	// Lucky Lady's Charm	GLLC0500B
		lotteries.put("GHCL0500B", "LOT_HCL_BONUS");	// Hot Cherry	GHCL0500B
		lotteries.put("GDPL0100B", "LOT_DPL_BONUS");	// Dolphin's Pearl	GDPL0100B
		lotteries.put("GBOR0122B", "LOT_BOR_BONUS");	// Book of Ra	GBOR0122B

		// Novolot-2

		lotteries.put("GCOL0500B", "LOT_COL_BONUS");	// Columbus	GCOL0500B
		lotteries.put("GMGM0500B", "LOT_MGM_BONUS");	// The Money Game	GMGM0500B
		lotteries.put("GPHG0500B", "LOT_PHG_BONUS");	// Pharaon's Gold II	GPHG0500B
		lotteries.put("GAHT0500B", "LOT_AHT_BONUS");	// Always Hot	GAHT0500B
		lotteries.put("GSHR0500B", "LOT_SHR_BONUS");	// Sharky	GSHR0500B
		lotteries.put("GDTR0500B", "LOT_DTR_BONUS");	// Diamond Trio	GDTR0500B

		// Novolot-3

		/*
		lotteries.put("GUHT0500B", "LOT_UHT_BONUS");	// Ultra Hot	GUHT0500B
		lotteries.put("GBQM0500B", "LOT_BQM_BONUS");	// Bungee Monkey	GBQM0500B
		lotteries.put("GKOC0500B", "LOT_KOC_BONUS");	// King of Cards	GKOC0500B
		lotteries.put("GKSQ0500B", "LOT_KSQ_BONUS");	// Knight's Quest	GKSQ0500B
		lotteries.put("GMMN0500B", "LOT_MMN_BONUS");	// Magic Money	GMMN0500B
		lotteries.put("GQOH0500B", "LOT_QOH_BONUS");	// Queen of Hearts	GQOH0500B
		*/

		// Novolot-4

		lotteries.put("GBGB0500B", "LOT_BGB_BONUS");	// Bananas Go Bahamas	GBGB0500B
		lotteries.put("GFCT0500B", "LOT_FCT_BONUS");	// First Class Traveller	GFCT0500B
		lotteries.put("GGRG0500B", "LOT_GRG_BONUS");	// Gryphon's Gold	GGRG0500B
		lotteries.put("GJJL0500B", "LOT_JJL_BONUS");	// Just Jewels	GJJL0500B
		lotteries.put("GOLB0500B", "LOT_OLB_BONUS");	// Oliver's Bar	GOLB0500B
		lotteries.put("GROI0500B", "LOT_ROI_BONUS");	// Riches of India	GROI0500B

		/*
		// Altolot-1

		lotteries.put("G3WS0500B", "LOT_3WS_BONUS");	// 3 Wishes	G3WS0500B
		lotteries.put("GCRF0500B", "LOT_CRF_BONUS");	// Crazy Fruits FG	GCRF0500B
		lotteries.put("GSPM0500B", "LOT_SPM_BONUS");	// Spell Master	GSPM0500B
		lotteries.put("GDMK0500B", "LOT_DMK_BONUS");	// Dream Maker	GDMK0500B
		lotteries.put("GBBB0500B", "LOT_BBB_BONUS");	// Big Blue Bucks	GBBB0500B
		lotteries.put("GKTR0500B", "LOT_KTR_BONUS");	// Kremlin Treasure	GKTR0500B

		// Igralot-1

		lotteries.put("GMON0500B", "LOT_MON_BONUS");
		lotteries.put("GFRU0500B", "LOT_FRU_BONUS");
		lotteries.put("GHUN0500B", "LOT_HUN_BONUS");
		lotteries.put("GCLI0500B", "LOT_CLI_BONUS");
		lotteries.put("GRES0500B", "LOT_RES_BONUS");
		lotteries.put("GGAR0500B", "LOT_GAR_BONUS");
		*/
	}


	private void initializeGames() {

		games.put("GBOR0122", Protocol.LONGTICKET);
		games.put("GLLC0500", Protocol.LONGTICKET);
		games.put("GDPL0100", Protocol.LONGTICKET);
		games.put("GBORSLOT", Protocol.SLOT);
		games.put("GBOR0123", Protocol.SHORTTICKET);
		games.put("GLLCSLOT", Protocol.SLOT);
		games.put("GDPLSLOT", Protocol.SLOT);
		games.put("GMPL0500", Protocol.LONGTICKET);
		games.put("GSHL0500", Protocol.LONGTICKET);
		games.put("GHCL0500", Protocol.LONGTICKET);

		games.put("GSHL0500B", Protocol.LONGTICKETBONUS);
		games.put("GMPL0500B", Protocol.LONGTICKETBONUS);
		games.put("GLLC0500B", Protocol.LONGTICKETBONUS);
		games.put("GHCL0500B", Protocol.LONGTICKETBONUS);
		games.put("GDPL0100B", Protocol.LONGTICKETBONUS);
		games.put("GBOR0122B", Protocol.LONGTICKETBONUS);

		games.put("GCOL0500B", Protocol.LONGTICKETBONUS);
		games.put("GMGM0500B", Protocol.LONGTICKETBONUS);
		games.put("GPHG0500B", Protocol.LONGTICKETBONUS);
		games.put("GAHT0500B", Protocol.LONGTICKETBONUS);
		games.put("GSHR0500B", Protocol.LONGTICKETBONUS);
		games.put("GDTR0500B", Protocol.LONGTICKETBONUS);

		// Novolot-3
		/*
		games.put("GUHT0500B", Protocol.LONGTICKETBONUS);	// Ultra Hot	GUHT0500B
		games.put("GBQM0500B", Protocol.LONGTICKETBONUS);	// Bungee Monkey	GBQM0500B
		games.put("GKOC0500B", Protocol.LONGTICKETBONUS);	// King of Cards	GKOC0500B
		games.put("GKSQ0500B", Protocol.LONGTICKETBONUS);	// Knight's Quest	GKSQ0500B
		games.put("GMMN0500B", Protocol.LONGTICKETBONUS);	// Magic Money	GMMN0500B
		games.put("GQOH0500B", Protocol.LONGTICKETBONUS);	// Queen of Hearts	GQOH0500B
		*/

		// Novolot-4

		games.put("GBGB0500B", Protocol.LONGTICKETBONUS);	// Bananas Go Bahamas	GBGB0500B
		games.put("GFCT0500B", Protocol.LONGTICKETBONUS);	// First Class Traveller	GFCT0500B
		games.put("GGRG0500B", Protocol.LONGTICKETBONUS);	// Gryphon's Gold	GGRG0500B
		games.put("GJJL0500B", Protocol.LONGTICKETBONUS);	// Just Jewels	GJJL0500B
		games.put("GOLB0500B", Protocol.LONGTICKETBONUS);	// Oliver's Bar	GOLB0500B
		games.put("GROI0500B", Protocol.LONGTICKETBONUS);	// Riches of India	GROI0500B

		/*
		// Altolot-1

		games.put("G3WS0500B", Protocol.LONGTICKETBONUS);	// 3 Wishes	G3WS0500B
		games.put("GCRF0500B", Protocol.LONGTICKETBONUS);	// Crazy Fruits FG	GCRF0500B
		games.put("GSPM0500B", Protocol.LONGTICKETBONUS);	// Spell Master	GSPM0500B
		games.put("GDMK0500B", Protocol.LONGTICKETBONUS);	// Dream Maker	GDMK0500B
		games.put("GBBB0500B", Protocol.LONGTICKETBONUS);	// Big Blue Bucks	GBBB0500B
		games.put("GKTR0500B", Protocol.LONGTICKETBONUS);	// Kremlin Treasure	GKTR0500B

		// Igrasoft-1

		games.put("GMON0500B", Protocol.IGRASOFT);
		games.put("GFRU0500B", Protocol.IGRASOFT);
		games.put("GHUN0500B", Protocol.IGRASOFT);
		games.put("GCLI0500B", Protocol.IGRASOFT);
		games.put("GRES0500B", Protocol.IGRASOFT);
		games.put("GGAR0500B", Protocol.IGRASOFT);
		*/

	}


	private GameRegistry() {
		services  = new HashMap<String, GameService>();
		games     = new HashMap<String, String>();
		lotteries = new HashMap<String, String>();

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

		initializeLotteries();
		initializeGames();

	}

	public GameService get(String game) {
		return services.get(game);
	}

	public HashMap<String, GameService> getRegistry() {
		return services;
	}

	public void init() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			if (ds != null) {
				synchronized(ds) {
					conn = ds.getConnection();
				}

				ps = conn.prepareStatement("SELECT A.GAME_ID, A.GAME_TYPE, A.NAME, A.DENOM, B.LOTTERY_ID, B.PRICE FROM GAME_METADATA A LEFT JOIN LOTTERY_METADATA B ON A.GAME_ID = B.GAME_ID");

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
						} else if (gametype.equals("LONGTICKETBONUS")) {
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
	}

	public HashMap<String, String> getGames() {
		return games;
	}

	public HashMap<String, String> getLotteries() {
		return lotteries;
	}

	public String getGametype(String game) {
		return games.get(game);
	}

	public GameAvailability[] getGameAvailability(String terminal) {

		Connection conn = null;
		ArrayList<GameAvailability> list = new ArrayList<GameAvailability>();

		try {
			if (ds != null) {
				synchronized(ds) {
					conn = ds.getConnection();
				}

				if (conn == null) {
					logger.log(Level.SEVERE, "Can not get Connection from DataSource");
					return null;
				}

				PreparedStatement ps = conn.prepareStatement("SELECT GAME, LOTTERY, PRICE, AVAILABLE FROM AVAILABILITY ORDER BY GAME, PRICE");

				ResultSet rs = ps.executeQuery();

				while (rs.next()) {
					GameAvailability element = new GameAvailability(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getBoolean(4));
					list.add(element);
				}

				return list.toArray(new GameAvailability[]{});

			} else {
				logger.log(Level.SEVERE, "Can not get connection from DataSource; ds == null");
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception trying to retrieve game availability for terminal " + terminal, e);
		} finally {
			try {
				conn.close();
			} catch(SQLException e) {}
		}

		return null;
	}
}
