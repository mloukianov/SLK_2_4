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
 * Set of game stops / plays for single lottery ticket with multiple plays.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision:  $ $Date:  $
 * @see
 */
public class BorGameStopsVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -6512373724892719887L;
	private int[] stops;
	private int[] lines;
	private int scatter;
	private int specialSymbol;
	private int specialSymbolWin;
	private int currentFreeGamesCount;
	private int freeGamesNumber;
	private BorGameStopsVO[] bonusSpins;
	private int doubleup;
	private int reelsetno;

	private int[] bonuses = new int[0];
	private int size = 0;


	public BorGameStopsVO(int[] stops, int[] lines, int scatter, int specialSymbol, int specialSymbolWin, int currentFreeGamesCount, int freeGamesNumber, int doubleup, int reelsetno) {
		this.stops = stops;
		this.lines = lines;
		this.scatter = scatter;
		this.specialSymbol = specialSymbol;
		this.specialSymbolWin = specialSymbolWin;
		this.currentFreeGamesCount = currentFreeGamesCount;
		this.freeGamesNumber = freeGamesNumber;
		this.doubleup = doubleup;
		this.reelsetno = reelsetno;
	}

	public BorGameStopsVO() {}

	public int[] getStops() {
		return stops;
	}

	public void setStops(int[] stops) {
		this.stops = stops;
	}

	public void addBonus(int bonus) {
		increaseCapacity();
		bonuses[size - 1] = bonus;
	}

	private void increaseCapacity() {
		size ++;
		int[] temp = new int[size];
		System.arraycopy(bonuses, 0, temp, 0, size-1);
		bonuses = temp;
	}

	public int[] getBonuses() {
		return this.bonuses;
	}

	public void setBonuses(int[] bonuses) {
		this.bonuses = bonuses;
	}

	public int[] getLines() {
		return lines;
	}

	public void setLines(int[] lines) {
		this.lines = lines;
	}

	public int getScatter() {
		return scatter;
	}

	public void setScatter(int scatter) {
		this.scatter = scatter;
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

	public void setBonusSpins(BorGameStopsVO[] bonusSpins) {
		this.bonusSpins = bonusSpins;
	}

	public BorGameStopsVO[] getBonusSpins() {
		return this.bonusSpins;
	}

	/**
	 * @return the doubleup
	 */
	public int getDoubleup() {
		return doubleup;
	}

	/**
	 * @param doubleup the doubleup to set
	 */
	public void setDoubleup(int doubleup) {
		this.doubleup = doubleup;
	}

	/**
	 * @return the reelsetno
	 */
	public int getReelsetno() {
		return reelsetno;
	}

	/**
	 * @param reelsetno the reelsetno to set
	 */
	public void setReelsetno(int reelsetno) {
		this.reelsetno = reelsetno;
	}
}
