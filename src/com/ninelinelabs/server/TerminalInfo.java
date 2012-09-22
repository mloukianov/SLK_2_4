/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TerminalInfo.java 102 2011-05-28 07:17:12Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 */
package com.ninelinelabs.server;

public class TerminalInfo {
	private String name;
	private int terminalid;
	private int operday;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the terminalid
	 */
	public int getTerminalid() {
		return terminalid;
	}
	/**
	 * @param terminalid the terminalid to set
	 */
	public void setTerminalid(int terminalid) {
		this.terminalid = terminalid;
	}
	/**
	 * @return the operday
	 */
	public int getOperday() {
		return operday;
	}
	/**
	 * @param operday the operday to set
	 */
	public void setOperday(int operday) {
		this.operday = operday;
	}
}
