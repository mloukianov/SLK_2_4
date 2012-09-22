/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.ninelinelabs.protocol.hpsp.Protocol;

public class AuthenticationRequestHandler /*implements Handler*/ {
	
	public static final Logger logger = Logger.getLogger(AuthenticationRequestHandler.class.getName());

	
	public String getMessageType() {
		return Protocol.AUTH_REQ;
	}

	
	public void handle(DataInputStream ds, DataOutputStream dos, String session) throws IOException {
		
		@SuppressWarnings("unused")
		String terminal = ds.readUTF();
		String authType = ds.readUTF();

		if (authType.equals(Protocol.PINNO)) {
			// handle pin number authentication
			/*
			String pin = ds.readUTF();

			logger.log(Level.INFO, "Session created: " + sessionId);
			logger.log(Level.INFO, sessionId + ": AUTH_REQ message received from " + terminal + ": AUTH TYPE = " + authType);

			int role = RequestProcessor.getRoleForCard(pin);

			if (role == -1) {
				// role not found; send back an error
				logger.log(Level.SEVERE, sessionId + ": cannot find role for PIN " + pin);
				// TODO: log authentication error into security log
				sendCardNotFoundResponse(terminal, pin, dos);
			} else if (role == 1) {
				
				lastpin = pin;
				SessionManager.getSessionManager().setParam(sessionId, "lastpin", this.lastpin);

				AuthenticationResult auth = RequestProcessor.processAuthenticationPINRequest(terminal, pin);
				
				if (auth.isAuthenticated()) {
					
					this.terminal = terminal;
					SessionManager.getSessionManager().setParam(sessionId, "terminal", this.terminal);
					
					this.cardno = pin;
					SessionManager.getSessionManager().setParam(sessionId, "cardno", this.cardno);
					
					this.lastpin = pin;
					SessionManager.getSessionManager().setParam(sessionId, "lastpin", this.lastpin);
					
					lastpins.put(this.terminal, this.lastpin);
					
					logger.log(Level.INFO, sessionId + ": PIN account balance moved to terminal balance:" + "\n" +
							               sessionId + ":    PIN  accnt bal = 0" + "\n" +
							               sessionId + ":    TERM accnt bal = " + auth.getAmount());
	
					int balance = auth.getAmount();
	
					sendAuthenticationResponse(terminal, balance, dos);
	
					setState(ConnectionSession.State.LOBBY_STATE);
					
				} else {
					logger.log(Level.SEVERE, "Card has wrong state: pin = " + pin);
					sendCardNotFoundResponse(terminal, pin, dos);
				}
			} else {
				logger.log(Level.INFO, sessionId + ": found service card for PIN " + pin + "; processing role = " + role);

				// TODO: log successful auth with tech card into security log

				sendTechCardResponse(terminal, role, dos);
			}
			*/

		} else if (authType.equals(Protocol.CCARD)) {
			/*
			// handle credit card authentication
			@SuppressWarnings("unused")
			String cardNumber = ds.readUTF();
			@SuppressWarnings("unused")
			String cardhodlerName = ds.readUTF();
			@SuppressWarnings("unused")
			String cvv2Code = ds.readUTF();
			@SuppressWarnings("unused")
			String expDate = ds.readUTF();
			*/
			return;
		} else if (authType.equals(Protocol.SMART)) {
			/*
			// handle smart card authentication
			@SuppressWarnings("unused")
			String cardNumber = ds.readUTF();

			// send the response back to the client
			 * 
			 */
			return;
		} else {
			// Unknown auth type; return error
			return;
		}

		return;
	}

	/*
	private void sendTechCardResponse(String terminal, int role, DataOutputStream dos) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(Protocol.TCRD_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeInt(role);							// role - int
		
		this.sendMessage(bos, dos, ddos);
	}

	
	private void sendCardNotFoundResponse(String terminal, String pin, DataOutputStream dos) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(Protocol.NFND_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeUTF(pin);							// role - int
		
		this.sendMessage(bos, dos, ddos);
	}

	
	private void sendAuthenticationResponse(String terminal, int balance, DataOutputStream dos)
																					throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(Protocol.AUTH_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeInt(balance);							// terminal balance - int
		
		this.sendMessage(bos, dos, ddos);
	}
	*/


}
