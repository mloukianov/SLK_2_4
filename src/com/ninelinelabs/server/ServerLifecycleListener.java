/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ServerLifecycleListener.java 81 2011-05-20 01:54:57Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 *
 */
package com.ninelinelabs.server;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * Servlet lifecycle listener implementation. Used to start and stop socket server
 * in Tomcat.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 81 $ $Date: 2011-05-19 20:54:57 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class ServerLifecycleListener implements LifecycleListener {
	private SocketServer server;

	public static final int POOL_SIZE = 400;

	private static final Logger logger = Logger.getLogger(ServerLifecycleListener.class.getName());


	public ServerLifecycleListener() {
		server = new SocketServer("tomcat socket server; port 9111", 9111, POOL_SIZE);
	}

	public void lifecycleEvent(LifecycleEvent event) {
		if (event.getType().equals(Lifecycle.START_EVENT)) {
			logger.log(Level.INFO, "Lifecycle.START_EVENT; component = " + event.getLifecycle() + " with object: " + event.getSource());
		} else if (event.getType().equals(Lifecycle.STOP_EVENT)) {
			server.stop();
		} else if (event.getType().equals(Lifecycle.INIT_EVENT)) {
			logger.log(Level.INFO, "Lifecycle.INIT_EVENT; component = " + event.getLifecycle() + " with object: " + event.getSource());
		} else if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
			try {
				logger.log(Level.INFO, "Lifecycle.AFTER_START_EVENT; component = " + event.getLifecycle() + " with object: " + event.getSource());
				server.init();
				server.start();
			} catch(IOException ioe) {
				logger.log(Level.SEVERE, "Can not start the server; IOException caught", ioe);
			}
		} else if (event.getType().equals(Lifecycle.BEFORE_START_EVENT)) {
			logger.log(Level.INFO, "Lifecycle.BEFORE_START_EVENT; component = " + event.getLifecycle() + " with object: " + event.getSource());
		} else if (event.getType().equals(Lifecycle.PERIODIC_EVENT)) {
			logger.log(Level.INFO, "Lifecycle.PERIODIC_EVENT; component = " + event.getLifecycle() + " with object: " + event.getSource());
		}
	}
}
