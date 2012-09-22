/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TicketLoggingStage.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 4, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.lottery;

/**
 * An intermediary between LinkedBlockingQueue<Ticket> and SynchronizedQueue<Ticket>.
 * This stage performs logging of last ticket served to the application.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class TicketLoggingStage implements Runnable {

	/**
	 *
	 */
	public TicketLoggingStage() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub

	}

}
