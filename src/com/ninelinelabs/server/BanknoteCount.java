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
 * July 01, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Bank notes count value object
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 60 $ $Date: 2011-05-19 20:22:32 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class BanknoteCount {
	private int denomination;
	private int count;

	public BanknoteCount(int denomination, int count) {
		this.denomination = denomination;
		this.count = count;
	}

	public int getDenomination() {
		return this.denomination;
	}

	public int getCount() {
		return this.count;
	}
}
