/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: TerminalRecord.java 103 2011-05-28 07:17:28Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 */
package com.ninelinelabs.server;

public class TerminalRecord {
	private String cardno;
	private String serialno;
	private String assetid;
	private String hallname;
	private String description;
	private boolean activated;
	private int terminalid;
	private String gameset;
	private int denomination;

	private String ticket;
	private int win;
	private int timestamp;
	private boolean print;

	public TerminalRecord() {}

	/**
	 * @return the cardno
	 */
	public String getCardno() {
		return cardno;
	}

	/**
	 * @param cardno the cardno to set
	 */
	public void setCardno(String cardno) {
		this.cardno = cardno;
	}

	/**
	 * @return the serialno
	 */
	public String getSerialno() {
		return serialno;
	}

	/**
	 * @param serialno the serialno to set
	 */
	public void setSerialno(String serialno) {
		this.serialno = serialno;
	}

	/**
	 * @return the assetid
	 */
	public String getAssetid() {
		return assetid;
	}

	/**
	 * @param assetid the assetid to set
	 */
	public void setAssetid(String assetid) {
		this.assetid = assetid;
	}

	/**
	 * @return the hallname
	 */
	public String getHallname() {
		return hallname;
	}

	/**
	 * @param hallname the hallname to set
	 */
	public void setHallname(String hallname) {
		this.hallname = hallname;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the activated
	 */
	public boolean isActivated() {
		return activated;
	}

	/**
	 * @param activated the activated to set
	 */
	public void setActivated(boolean activated) {
		this.activated = activated;
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

	public void setGameset(String gameset) {
		this.gameset = gameset;
	}

	public String getGameset() {
		return this.gameset;
	}

	public int getDenomination() {
		return denomination;
	}

	public void setDenomination(int denomination) {
		this.denomination = denomination;
	}

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public int getWin() {
		return win;
	}

	public void setWin(int win) {
		this.win = win;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isPrint() {
		return print;
	}

	public void setPrint(boolean print) {
		this.print = print;
	}
}
