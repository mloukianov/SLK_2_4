/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Denomination.java 235 2011-09-08 03:07:22Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 25, 2011 mloukianov Created
 * Aug 28, 2011 mloukianov Fixed the formatting
 *
 */
package com.ninelinelabs.server;

/**
 * Denomination value object
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 235 $ $Date: 2011-09-07 22:07:22 -0500 (Wed, 07 Sep 2011) $
 * @see
 */
public class Denomination {

	private int denomination;
	private String description;
	private int exchangeRate;
	private String currencyAccount;
	private String creditsAccount;
	
	
	/**
	 * @return the denomination
	 */
	public int getDenomination() {
		return denomination;
	}

	/**
	 * @param denomination the denomination to set
	 */
	public void setDenomination(int denomination) {
		this.denomination = denomination;
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
	 * @return the exchangeRate
	 */
	public int getExchangeRate() {
		return exchangeRate;
	}

	/**
	 * @param exchangeRate the exchangeRate to set
	 */
	public void setExchangeRate(int exchangeRate) {
		this.exchangeRate = exchangeRate;
	}

	/**
	 * @return the currencyAccount
	 */
	public String getCurrencyAccount() {
		return currencyAccount;
	}

	/**
	 * @param currencyAccount the currencyAccount to set
	 */
	public void setCurrencyAccount(String currencyAccount) {
		this.currencyAccount = currencyAccount;
	}

	/**
	 * @return the creditsAccount
	 */
	public String getCreditsAccount() {
		return creditsAccount;
	}

	/**
	 * @param creditsAccount the creditsAccount to set
	 */
	public void setCreditsAccount(String creditsAccount) {
		this.creditsAccount = creditsAccount;
	}
}
