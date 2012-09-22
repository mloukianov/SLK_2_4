/*
 * Copyright (C) 2008-2009, Nine Line Labs, Inc.
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs Inc. or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SessionTracker.java 94 2011-05-28 07:13:45Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Feb 12, 2009 mloukianov Created
 * Feb 26, 2009 mloukianov Removed some extra logging and added session id to logging
 * 
 */
package com.ninelinelabs.server;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 * A class implementing session tracker for terminal sessions.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 94 $ $Date: 2011-05-28 02:13:45 -0500 (Sat, 28 May 2011) $
 * @see 
 */
public class SessionTracker implements Runnable {
	private ConnectionRunnable connection;
	private int lastping = 0;
	private ScheduledThreadPoolExecutor executor;
	private boolean clean = false;
	
	private static final Logger logger = Logger.getLogger(SessionTracker.class.getName());

	public SessionTracker(ConnectionRunnable connection) {
		this.connection = connection;
	}
	
	public void run() {
		
		if (clean) return;
		
		if (lastping == 0) {
			lastping = connection.getLastPing();
			logger.log(Level.FINEST, "Last ping time unknown; saving last ping time as " + lastping);
			return;
		}
		
		logger.log(Level.FINEST, "Checking for stale session; last ping time " + lastping);
		
		if (lastping == connection.getLastPing()) {
			logger.log(Level.INFO, "Found stale session: " + connection.getSessionId() + "; starting session cleanup");
			connection.cleanupSession();
			clean = true;
		} else {
			lastping = connection.getLastPing();
		}
	}
	
	public void start() {

		executor = new ScheduledThreadPoolExecutor(1);
		executor.scheduleAtFixedRate(this, 15, 15, TimeUnit.SECONDS);
		
		if (connection.getSessionId() != null)
			logger.log(Level.INFO, "Started SessionTracker for session " + connection.getSessionId());
	}
	
	public void stop() {
		executor.shutdown();
	}
}
