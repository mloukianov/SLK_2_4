/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: PlayRequestHandler.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
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

import java.util.logging.Logger;
import java.util.logging.Level;

import com.ninelinelabs.lottery.generator.vo.LongTicketSpinVO;
import com.ninelinelabs.lottery.generator.vo.LongTicketVO;
import com.ninelinelabs.protocol.hpsp.Protocol;
import com.ninelinelabs.server.GameRegistry;

import com.ninelinelabs.server.SessionManager;

/**
 * PLAY_REQ message handler
 * 
 * @author mloukianov
 *
 */
public class PlayRequestHandler /*implements Handler*/ {
	
	private static final Logger logger = Logger.getLogger(PlayRequestHandler.class.getName());

	public String getMessageType() {
		return Protocol.PLAY_REQ;
	}

	public void handle(DataInputStream dis, DataOutputStream dos, String session) throws IOException {
		
		SessionManager sessionManager = SessionManager.getSessionManager();

		@SuppressWarnings("unused")
		String terminal = dis.readUTF();
		String game     = dis.readUTF();
		@SuppressWarnings("unused")
		int nextBetSeq  = dis.readInt();
		@SuppressWarnings("unused")
		int bet         = dis.readInt();
		int totalLines  = dis.readInt();

		int lines[] = new int[totalLines];

		for (int i = 0; i < totalLines; i ++) { lines[i] = dis.readInt(); }
		
		@SuppressWarnings("unused")
		int totalbet = dis.readInt();
		@SuppressWarnings("unused")
		String visualization  = dis.readUTF();
		
		String gametype = GameRegistry.getInstance().getGametype(game);
		
		if (gametype == null || gametype.equals("")) {
			logger.log(Level.SEVERE, "Can not find gametype for game " + game);
			throw new IOException("Can not find gametype for game " + game);
		}
		
		if (gametype.equals(Protocol.LONGTICKET) || gametype.equals(Protocol.SLOT)) {
			LongTicketVO ticket = (LongTicketVO)sessionManager.getParam(session, "ticket");
			
			if (ticket.isBonus()) {
				// this never happens for LONGTICKET type
			} else {
				/*
				if (ticket.getPointer() >= ticket.size()) {
					// end of ticket; send TIKT_END message
					sendEndTicketResponse(dos, terminal, game);

				} else {

					LongTicketSpinVO spin = ticket.getSpin(totalLines, bet, ticket.getPointer());
					currentSpin = spin;
					ticket.movePointer();

					int totalWin = 0;
					int linesPlayed = 0;

					for (int i = 0; i < totalLines; i++) {
						
						totalWin += spin.getLines()[i]*lines[i]*bet;
						linesPlayed += lines[i];
					}

					totalWin += linesPlayed*bet*spin.getScatter();

					RequestProcessor.processPlayLongTicketRequest(terminal, game, cardno, bet*linesPlayed, totalWin);

					logger.log(Level.INFO, session + ": played one regular spin on TICKET # " + ticket.getTicketNo() + ": regular play # " + (ticket.getPointer() - 1) + "\n" +
					                       session + ":    BET = " + bet*linesPlayed + "; WIN = " + totalWin);

					sendPlayResponse(dos, terminal, game, nextBetSeq, totalWin, spin.getStops(), spin.getBonuses(), spin.getScatter(), spin.getLines(), (ticket.getPointer() + 2), 0, 0, 0);
				}
				*/
			}
		}
	}
	
	@SuppressWarnings("unused")
	private int getTotalWin(LongTicketSpinVO spin) {
		int totalWin = 0;
		int linesPlayed = 0;
		
		return totalWin;
	}

}
