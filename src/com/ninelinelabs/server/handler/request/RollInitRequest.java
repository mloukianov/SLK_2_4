/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: RollInitRequest.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Sep 16, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.server.handler.request;

/**
 * Ticket roll initialization request
 * 
 * @author mloukianov
 */
public class RollInitRequest {
	
	public static final String MSGTYPE = "ROLL_INIT";
	
	private String terminal;
	private String rollid;
	
	public RollInitRequest(String terminal, String rollid) {
		this.terminal = terminal;
		this.rollid = rollid;
	}
	
	/**
	 * Get terminal name for this request
	 * 
	 * @return  terminal name as string
	 */
	public String getTerminal() {
		return this.terminal;
	}
	
	/**
	 * Get tickets roll id
	 * 
	 * @return  tickets roll id as string
	 */
	public String getRollid() {
		return this.rollid;
	}
}
