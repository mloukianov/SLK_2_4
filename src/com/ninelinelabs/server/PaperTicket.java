/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: PaperTicket.java 90 2011-05-28 07:09:59Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

/**
 * Utility class for parsing lottery ticket number
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 90 $ $Date: 2011-05-28 02:09:59 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class PaperTicket {

	private String lottery;
	private String series;
	private String ticket;
	private String ticketprefix;
	private int ticketno;
	private int price;


	public static PaperTicket parseTicket(String barcode) {

		PaperTicket instance = new PaperTicket();

		instance.setLottery(barcode.substring(0, 2));

		instance.setTicket(barcode.substring(5, barcode.length()));

		instance.setTicketprefix(barcode.substring(barcode.length()-7, barcode.length()-4));

		instance.setTicketno(Integer.parseInt(barcode.substring(barcode.length()-4, barcode.length())));

		if (instance.getLottery().equals("11")) {
			instance.setPrice(2000);
			instance.setSeries(barcode.substring(2, 5));

		} else if (instance.getLottery().equals("22")) {
			instance.setPrice(10000);
			instance.setSeries(barcode.substring(3, 5));

		} else if (instance.getLottery().equals("33")) {
			instance.setPrice(20000);
			instance.setSeries(barcode.substring(3, 5));

		}

		return instance;
	}


	/**
	 * @return the lottery
	 */
	public String getLottery() {
		return lottery;
	}
	/**
	 * @param lottery the lottery to set
	 */
	public void setLottery(String lottery) {
		this.lottery = lottery;
	}
	/**
	 * @return the series
	 */
	public String getSeries() {
		return series;
	}
	/**
	 * @param series the series to set
	 */
	public void setSeries(String series) {
		this.series = series;
	}
	/**
	 * @return the ticket
	 */
	public String getTicket() {
		return ticket;
	}
	/**
	 * @param ticket the ticket to set
	 */
	public void setTicket(String ticket) {
		this.ticket = ticket;
	}
	/**
	 * @return the ticketno
	 */
	public int getTicketno() {
		return ticketno;
	}
	/**
	 * @param ticketno the ticketno to set
	 */
	public void setTicketno(int ticketno) {
		this.ticketno = ticketno;
	}


	/**
	 * @return the ticketprefix
	 */
	public String getTicketprefix() {
		return ticketprefix;
	}


	/**
	 * @param ticketprefix the ticketprefix to set
	 */
	public void setTicketprefix(String ticketprefix) {
		this.ticketprefix = ticketprefix;
	}


	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}


	/**
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}
}
