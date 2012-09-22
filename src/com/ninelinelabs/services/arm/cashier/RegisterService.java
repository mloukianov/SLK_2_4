/*
 * Copyright (C) 2008-2011, Nine Line Labs, LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs, LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: RegisterService.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * May 4, 2009 mloukianov Created
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

/**
 * A class representing
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class RegisterService {

	public static DataSource ds = null;

	private static final Logger logger = Logger.getLogger(RegisterService.class.getName());

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

	public RegisterService() {

	}

	public List<String> getFreeRegistersForUser(String user, String role) {
		return new ArrayList<String>();
	}

	public String depositMoney(String document, long amount, String comment, int cashier) {
		return "deposit-reference";
	}

	public String withdrawMoney(String document, long amount, String comment, int cashier) {
		return "withdrawal-reference";
	}

	public String countSatelliteRegister(int satellite, long amount, String comment, int cashier) {
		return "satellite-reference";
	}

	public String fileDiscrepancyReport() {return ""; }
}
