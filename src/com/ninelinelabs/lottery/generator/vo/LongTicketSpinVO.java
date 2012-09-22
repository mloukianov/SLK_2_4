/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LongTicketSpinVO.java $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.lottery.generator.vo;

import java.io.Serializable;

/**
 * An class representing spin in a lottery ticket.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 53 $ $Date: 2011-05-19 20:11:04 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class LongTicketSpinVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8527158634244690771L;
	private int[] stops;   // 255 -> byte
	private int[] lines;   // 65,536 -> 2 bytes
	private int scatter;
	private int doubleup;
	private int bonuses;
	private LongTicketSpinVO[] bonusSpins;

	private int pointer = 0;	// pointer points to the appropriate position in bonus spins

	public LongTicketSpinVO(int[] stops, int[] lines, int scatter, int doubleup, int bonuses) {
		this.stops = stops;
		this.lines = lines;
		this.scatter = scatter;
		this.doubleup = doubleup;
		this.bonuses = bonuses;
	}

	public int getPointer() {
		return this.pointer;
	}

	public void movePointer() {
		this.pointer++;
	}

	public void useDoubleUp() {
		this.doubleup--;
	}

	public void setLongTicketSpins(LongTicketSpinVO[] spins) {
		this.bonusSpins = spins;
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

	public int getBonuses() {
		return this.bonuses;
	}

	public LongTicketSpinVO[] getBonusSpins() {
		return this.bonusSpins;
	}
}
