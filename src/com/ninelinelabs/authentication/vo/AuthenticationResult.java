/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: AuthenticationResult.java 237 2011-09-08 03:11:27Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Aug 28, 2011 Changed javadoc class description
 *
 */
package com.ninelinelabs.authentication.vo;

import com.ninelinelabs.server.Denomination;

/**
 * Authentication result
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 237 $ $Date: 2011-09-07 22:11:27 -0500 (Wed, 07 Sep 2011) $
 * @see
 */
public class AuthenticationResult {
	private final String pin;
	private int amount;
	private final boolean authenticated;
	private boolean specialcard;

	private long clubId;
	private long specialCardId;

	private String drawingUrl;

	private Denomination denomination;

	public Denomination getDenomination() {
		return denomination;
	}

	public void setDenomination(Denomination denomination) {
		this.denomination = denomination;
	}

	public AuthenticationResult(String pin, int amount, boolean authenticated, boolean specialcard) {
		this.pin = pin;
		this.amount = amount;
		this.authenticated = authenticated;
		this.specialcard = specialcard;
	}

	public String getPin() {
		return pin;
	}

	public int getAmount() {
		return amount;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public boolean isSpecialcard() {
		return this.specialcard;
	}

	public void setSpecialCard(boolean specialcard) {
		this.specialcard = specialcard;
	}

	public long getClubId() {
		return clubId;
	}

	public void setClubId(long clubId) {
		this.clubId = clubId;
	}

	public long getSpecialCardId() {
		return specialCardId;
	}

	public void setSpecialCardId(long specialCardId) {
		this.specialCardId = specialCardId;
	}

	public String getDrawingUrl() {
		return drawingUrl;
	}

	public void setDrawingUrl(String drawingUrl) {
		this.drawingUrl = drawingUrl;
	}
}
