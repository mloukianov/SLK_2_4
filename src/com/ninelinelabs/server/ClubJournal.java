/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ClubJournal.java 63 2011-05-20 01:25:34Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * July 01, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Club journal operations constants
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 63 $ $Date: 2011-05-19 20:25:34 -0500 (Thu, 19 May 2011) $
 * @see
 */
public interface ClubJournal {

	public static final int DAILY_COUNT = 101;
	public static final int EXTRA_COUNT = 102;
	public static final int EMERGENCY_COUNT = 103;

	public static final int ROLL_INIT = 110;

	public static final int AUTH_ADMIN = 121;
	public static final int AUTH_TECH = 122;
	public static final int AUTH_MNGR = 123;

	public static final int LOGOUT_ADMIN = 126;
	public static final int LOGOUT_TECH = 127;
	public static final int LOGOUT_MNGR = 128;

	public static final int UNBLOCK_VLT_ADMIN = 130;

}
