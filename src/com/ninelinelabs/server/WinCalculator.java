/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: WinCalculator.java 110 2011-05-28 07:20:01Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 */
package com.ninelinelabs.server;

public interface WinCalculator {

	public int calculateSpinWin();

	public int calculateTotalFreeGamesWin();
}
