/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: PaperTicketRoll.java 91 2011-05-28 07:10:26Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Value object for paper ticket roll information
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 91 $ $Date: 2011-05-28 02:10:26 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class PaperTicketRoll {
	private String barcode;
	private int terminalid;
	private boolean initialized;
	private int nextTicket;


	/**
	 * @return the barcode
	 */
	public String getBarcode() {
		return barcode;
	}
	/**
	 * @param barcode the barcode to set
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
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
	 * @return the initialized
	 */
	public boolean isInitialized() {
		return initialized;
	}
	/**
	 * @param initialized the initialized to set
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
	/**
	 * @return the nextTicket
	 */
	public int getNextTicket() {
		return nextTicket;
	}
	/**
	 * @param nextTicket the nextTicket to set
	 */
	public void setNextTicket(int nextTicket) {
		this.nextTicket = nextTicket;
	}
}
