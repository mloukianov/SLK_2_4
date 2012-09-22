/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: AuthenticationService.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.services.arm.cashier;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;


public class AuthenticationService {

	public HashMap<String, String> tokens = new HashMap<String, String>();

	public static DataSource ds = null;

	private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());

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


	public AuthenticationService() {

	}

	public String authenticateEmployeeCard(String card, String pin) {
		// TODO: check that this card exists and role associated with the card is one of
		// the technical role (i.e. it is not a player card)
		// TODO: check that the PIN is correct
		// TODO: we need to start using PIN block, like in payment sys

		return "user-token-1234567890";
	}

	public List<String> getRolesForUser(String token) throws NullPointerException {
		String card = tokens.get(token);

		if (card == null) throw new NullPointerException("User token is not found or invalid");

		return new ArrayList<String>();
	}
}
