/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: $
 *
 * Date Author Changes
 * Oct 15, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.lottery.generator.vo.bor;

import java.io.Serializable;

/**
 * Single play spin in a lottery tickets
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 56 $ $Date: 2011-05-19 20:14:22 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class BorLongTicketSpinVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7255207589092043628L;
	private int[] stops;
	private int[] lines;
	private int scatter;
	private int doubleup;
	private int specialSymbol;
	private int specialSymbolWin;
	private int currentFreeGamesCount;
	private int freeGamesNumber;
	private int reelsetno;
	private BorLongTicketSpinVO[] bonusSpins;

	private int pointer = -1;	// pointer points to the appropriate position in bonus spins

	private int[] bonuses;


	public BorLongTicketSpinVO(int[] stops, int[] lines, int scatter, int doubleup, int specialSymbol,
			int specialSymbolWin, int currentFreeGamesCount, int freeGamesNumber, int reelsetno, int[] bonuses) {
		this.stops = stops;
		this.lines = lines;
		this.scatter = scatter;
		this.doubleup = doubleup;
		this.specialSymbol = specialSymbol;
		this.specialSymbolWin = specialSymbolWin;
		this.currentFreeGamesCount = currentFreeGamesCount;
		this.freeGamesNumber = freeGamesNumber;
		this.reelsetno = reelsetno;

		this.bonuses = bonuses;
	}

	/**
	 * Spin is a bonus spin when current free games count is more than zero
	 *
	 * @return   true is this is a bonus spin
	 */
	public boolean isBonus() {
		return (currentFreeGamesCount > 0);
	}

	public int[] getBonuses() {
		return this.bonuses;
	}

	public void setBonuses(int[] bonuses) {
		this.bonuses = bonuses;
	}

	public int[] getStops() {
		return this.stops;
	}

	public int[] getLines() {
		return this.lines;
	}

	public int getScatter() {
		return this.scatter;
	}

	public int getDoubleup() {
		return this.doubleup;
	}

	public int getSpecialSymbol() {
		return specialSymbol;
	}

	public void setSpecialSymbol(int specialSymbol) {
		this.specialSymbol = specialSymbol;
	}

	public int getSpecialSymbolWin() {
		return specialSymbolWin;
	}

	public void setSpecialSymbolWin(int specialSymbolWin) {
		this.specialSymbolWin = specialSymbolWin;
	}

	public int getCurrentFreeGamesCount() {
		return currentFreeGamesCount;
	}

	public void setCurrentFreeGamesCount(int currentFreeGamesCount) {
		this.currentFreeGamesCount = currentFreeGamesCount;
	}

	public int getFreeGamesNumber() {
		return freeGamesNumber;
	}

	public void setFreeGamesNumber(int freeGamesNumber) {
		this.freeGamesNumber = freeGamesNumber;
	}

	public int getPointer() {
		return pointer;
	}

	public void setPointer(int pointer) {
		this.pointer = pointer;
	}

	public void setStops(int[] stops) {
		this.stops = stops;
	}

	public void setLines(int[] lines) {
		this.lines = lines;
	}

	public void setScatter(int scatter) {
		this.scatter = scatter;
	}

	public void setDoubleup(int doubleup) {
		this.doubleup = doubleup;
	}

	public void useDoubleup() {
		this.doubleup--;
	}

	public int getReelsetno() {
		return reelsetno;
	}

	public void setReelsetno(int reelsetno) {
		this.reelsetno = reelsetno;
	}

	public void setBonusSpins(BorLongTicketSpinVO[] bonusSpins) {
		this.bonusSpins = bonusSpins;
	}

	public BorLongTicketSpinVO[] getBonusSpins() {
		return this.bonusSpins;
	}
}
