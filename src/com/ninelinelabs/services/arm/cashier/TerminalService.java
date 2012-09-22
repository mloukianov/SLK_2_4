/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TerminalService.java 224 2011-07-24 19:17:17Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * May 04, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * May 13, 2011 mloukianov Updated copyright statement
 * Jul 24, 2011 mloukianov Changed return type for methods from generic to parameterized type
 *
 */
package com.ninelinelabs.services.arm.cashier;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.List;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;

import com.ninelinelabs.server.TerminalInfo;

/**
 * A class representing
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 224 $ $Date: 2011-07-24 23:17:17 +0400 (Sun, 24 Jul 2011) $
 * @see
 */
public class TerminalService {
	public static DataSource ds = null;

	private static final Logger logger = Logger.getLogger(TerminalService.class.getName());

	static {
		try {
			final StandardServer server = (StandardServer)ServerFactory.getServer();
			final Context ctx = server.getGlobalNamingContext();

			if (ctx != null) {
				ds = (DataSource)ctx.lookup("jdbc/GameServerDB");
			} else {
				ds = null;
			}
		} catch (NamingException ne) {
			logger.log(Level.SEVERE, "Can not get DataSource; NamingException caught", ne);
		}
	}

	public TerminalService() {

	}

	// public methods of the service

	public List<TerminalInfo> getTerminalsForRegisterCount(String token) {
		return new ArrayList<TerminalInfo>();
	}

	public List<TerminalInfo> getTerminalsInfo(String token) {
		return new ArrayList<TerminalInfo>();
	}
}
