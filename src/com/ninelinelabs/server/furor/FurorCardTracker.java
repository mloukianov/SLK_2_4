/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id $
 *
 * Date Author Changes
 * Jun 12, 2011 mloukianov Created
 * Jun 14, 2011 mloukianov Renamed to FurorCardTracker
 * Aug 28, 2011 mloukianov Factored fields for current terminal/card session into FurorTracker class
 *
 */
package com.ninelinelabs.server.furor;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceClient;

import com.finstar.nsi.ws.drawingrequest.DrawingRequestPort;
import com.finstar.nsi.ws.drawingrequest.DrawingRequestService;
import com.finstar.nsi.ws.drawingrequest.WSInputDrawingRequest;
import com.finstar.nsi.ws.drawingrequest.WSOutputDrawingRequest;
import com.ninelinelabs.authentication.vo.AuthenticationResult;
import com.ninelinelabs.util.webservice.WebServiceUtils;

public class FurorCardTracker {

	public static final String WSDL_FILE = "/wsdl/drawingrequest_ws.wsdl";
	public static final String SERVICE_NAME= "DrawingRequestService";

	public static final Logger logger = Logger.getLogger(FurorCardTracker.class.getName());

	private DrawingRequestService service;
	private DrawingRequestPort port;

	private boolean specialcondition;
	private int denomination;
	private long cumulativeWin = 0L;

	private int bet;
	private int totalLines;
	private int[] lines = {0, 0, 0, 0, 0, 0, 0, 0, 0};	// 9 lines
	private int totalBet;

	// special cards support
	public static final int MAX_SPECIAL = 8000000;	// in kopecks
	public static final int MIN_SPECIAL = 5000000;	// in kopecks


	public FurorCardTracker() {

		resetSpecialCondition();
		initWebService();
	}

	private void initWebService() {

		try {
			WebServiceClient ann = DrawingRequestService.class.getAnnotation(WebServiceClient.class);
			URL wsdlURL = FurorCardTracker.class.getResource(WSDL_FILE);
			service = new DrawingRequestService(wsdlURL, new QName(ann.targetNamespace(), ann.name()));
			port = service.getDrawingRequestService();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception initializing " + SERVICE_NAME, e);
		}
	}


	public int getBet() {
		return bet;
	}


	public void setBet(int bet) {
		this.bet = bet;
	}


	public int getTotalLines() {
		return totalLines;
	}


	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}


	public int[] getLines() {
		return lines;
	}


	public void setLines(int[] lines) {
		this.lines = lines;
	}


	public int getTotalBet() {
		return totalBet;
	}


	public void setTotalBet(int totalBet) {
		this.totalBet = totalBet;
	}


	public boolean getSpecialCondition() {
		return specialcondition;
	}

	public void resetSpecialCondition() {
		this.specialcondition = false;
	}

	public void setDenomination(int denomination) {
		this.denomination = denomination;
	}

	public int getDenomination() {
		return this.denomination;
	}

	public long highWatermark() {
		return MAX_SPECIAL / denomination;
	}

	public long lowWatermark() {
		return MIN_SPECIAL / denomination;
	}

	public void decrementBet(long bet) {
		cumulativeWin -= bet;

		logger.log(Level.INFO, "Cumulative win is decremented by " + bet + "; cumulative win = " + cumulativeWin + "; money = " + cumulativeWin * denomination);
	}

	public void incrementWin(long win) {
		cumulativeWin += win;

		logger.log(Level.INFO, "Cumulative win is incremented by " + win + "; cumulative win = " + cumulativeWin + "; money = " + cumulativeWin * denomination);
	}

	public long getCumulativeWin() {
		return this.cumulativeWin;
	}

	public boolean stopPlay() {
		return (specialcondition && cumulativeWin > MIN_SPECIAL / denomination);
	}

	public boolean LOW_WATERMARK() {
		return (cumulativeWin > MIN_SPECIAL / denomination);
	}

	public boolean HIGH_WATERMARK() {
		return (cumulativeWin > MAX_SPECIAL / denomination);
	}

	public void resetLines() {

		for (int i = 0; i < this.totalLines; i++) {
			this.lines[i] = 1;
		}

		for (int i = this.totalLines; i < this.lines.length; i++ ) {
			this.lines[i] = 0;
		}
	}


	private boolean makeDrawingRequest(long club, long card) {

		try {

			WSInputDrawingRequest wsinput = new WSInputDrawingRequest();
			wsinput.setClubId(club);
			wsinput.setSpecialCardId(card);

			WSOutputDrawingRequest wsoutput = port.makeDrawingRequest(wsinput);

			return wsoutput.isRequestResult();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Caught exception when calling " + SERVICE_NAME, e);
		}

		return false;
	}


	/**
	 * Check special card status using web service
	 *
	 * @param auth	authentication parameters
	 *
	 * @return	true of special card status is active at this time
	 */
	public boolean checkSpecialCardStatus(AuthenticationResult auth) {

		if (auth.isSpecialcard()) {

			logger.log(Level.INFO, "special card found : pin = " + auth.getPin());

			try {

				WebServiceUtils.setEndpointUrl((BindingProvider) port, auth.getDrawingUrl() + SERVICE_NAME);

				logger.log(Level.INFO, "Calling drawing service at " + auth.getDrawingUrl() + SERVICE_NAME);

				specialcondition = makeDrawingRequest(auth.getClubId(), auth.getSpecialCardId());

				logger.log(Level.INFO, "Drawing service returned " + specialcondition);

				return specialcondition;

			} catch(Exception e) {
				logger.log(Level.SEVERE, "Caught exception ", e);
			}
		}

		return false;
	}
}
