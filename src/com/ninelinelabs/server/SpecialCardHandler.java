/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SpecialCardHandler.java 141 2011-06-22 14:55:51Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 */
package com.ninelinelabs.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ninelinelabs.lottery.generator.vo.bor.BorLongTicketSpinVO;

/**
 * Used to process special cards requests
 *
 * @author maxloukianov
 */
public class SpecialCardHandler {

	public static final int[] BETS = {1, 2, 3, 4, 5, 10, 15, 20, 30, 40, 50, 100};

	public static final int[] LINES     = {0, 1, 2, 3, 4, 5, 6, 7, 8};	// lines for 9-line machine

	public static final int[] POSITIONS = {0, 1, 2, 3, 4, 5, 6, 7, 8};	// positions on a line

	public static final int[][] LINEMASK = {
		                                    {0, 0, 0, 0, 0, 0, 0, 0, 0},	// LINE0
		                                    {1, 0, 0, 0, 0, 0, 0, 0, 0},	// LINE1
                                            {0, 1, 0, 0, 0, 0, 0, 0, 0},	// LINE2
                                            {0, 0, 1, 0, 0, 0, 0, 0, 0},	// LINE3
                                            {0, 0, 0, 1, 0, 0, 0, 0, 0},	// LINE4
                                            {0, 0, 0, 0, 1, 0, 0, 0, 0},	// LINE5
                                            {0, 0, 0, 0, 0, 1, 0, 0, 0},	// LINE6
                                            {0, 0, 0, 0, 0, 0, 1, 0, 0},	// LINE7
                                            {0, 0, 0, 0, 0, 0, 0, 1, 0},	// LINE8
                                            {0, 0, 0, 0, 0, 0, 0, 0, 1} 	// LINE9
										   };


	public static final Logger logger = Logger.getLogger(SpecialCardHandler.class.getName());



	/**
	 * Returns bet parameters (lines and bet per line) and win amount that makes it closest to the min win but not exceed max win
	 *
	 * @param maxwin		maximum win amount (in credits)
	 * @param l	current cumulative win amount (in credits)
	 * @param regular		regular spin object
	 *
	 * @return	SpecialCardWin object containing bet parameters
	 */
	public SpecialCardWin findClosestWin(int maxwin, long l, BorLongTicketSpinVO regular) {

		SpecialCardWin closestwin = null;

		SpecialCardWin[] wins = this.getAllPossibleWins(regular);

		for (SpecialCardWin win : wins) {
			int realwin = win.getWin() - win.getBet()*win.getLines();

			if ((l + realwin) < maxwin) {
				if (closestwin == null) {
					closestwin = win;
				}

				int closestrealwin = closestwin.getWin() - closestwin.getBet()*closestwin.getLines();

				if (realwin > closestrealwin) {
					closestwin = win;
				}
			}
		}

		return closestwin;
	}


	/**
	 * Get wins for all possible lines and win combinations
	 *
	 * @param regular	regular spin object
	 *
	 * @return	array of SpecialCardWin objects; each object contains lines, bet, and win amounts
	 */
	public SpecialCardWin[] getAllPossibleWins(BorLongTicketSpinVO regular) {

		ArrayList<SpecialCardWin> wins = new ArrayList<SpecialCardWin>();

		for (int lines = 1; lines <= 9; lines++ ) {
			for (int bet : SpecialCardHandler.BETS) {
				int win = calculateWin(lines, bet, regular);
				wins.add(new SpecialCardWin(lines, bet, win));
			}
		}

		return wins.toArray(new SpecialCardWin[]{});
	}


	/**
	 * Calculate total regular win for given spin, number of lines and bet per line amount
	 *
	 * @param lines		number of lines
	 * @param bet		bet per line amount
	 * @param regular	regular spin object
	 *
	 * @return	total regular win amount
	 */
	public int calculateWin(int lines, int bet, BorLongTicketSpinVO regular) {

		int totalwin = 0;

		// calculate win per lines
		for (int line = 1; line <= lines; line++)
			totalwin += calculateLineWin(line, regular);

		// add scattter win
		totalwin += regular.getScatter()*lines;

		// add all choice bonuses
		for (int bonus : regular.getBonuses())
			totalwin += bonus;

		// add all bonus games win
		if (regular.getFreeGamesNumber() > 0 && regular.getBonusSpins() != null)
			for (BorLongTicketSpinVO bonus : regular.getBonusSpins())
				totalwin += calculateWin(lines, bet, bonus);

		return totalwin * bet;
	}


	/**
	 * Return maximum number of successful double-ups for given regular spin
	 *
	 * @param regular		regular spin object
	 *
	 * @return		maximum number of successful double-ups
	 */
	public int getDoubleUps(BorLongTicketSpinVO regular) {
		return regular.getDoubleup();
	}


	/**
	 * Calculate line win for given line and bet per line amount
	 *
	 * @param line  line number
	 * @param bet   bet per line
	 * @param spin  spin information
	 *
	 * @return line win amount
	 */
	public int calculateLineWin(int line, BorLongTicketSpinVO spin) {

		int linewin = 0;

		for (int pos : SpecialCardHandler.POSITIONS)
			linewin += spin.getLines()[pos] * SpecialCardHandler.LINEMASK[line][pos];

		return linewin;
	}


	/**
	 * Returns drawing URL for DwingRequestService web service
	 *
	 * @param conn		database connection
	 *
	 * @return	URL for DrawingRequestService web service
	 *
	 * @throws SQLException
	 */
	public String getDrawingUrl(Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT URL FROM DRAWING_URL");

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			return rs.getString(1);
		} else {
			logger.log(Level.SEVERE, "Can not retrieve DRAWING_URL from server database");
			return null;
		}
	}
}
