/*
 * Copyright (C) 2008-2012, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ConnectionRunnable.java 283 2011-10-18 13:58:02Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Sep 12, 2008 mloukianov Created
 * Jan 27, 2009 mloukianov Added support for PING keep-alive protocol
 * Jan 27, 2009 mloukianov Added support for SessionTracker for stale session cleanup
 * Feb 03, 2009 mloukianov Added support for automatically numbering revisions, dates, and id
 * Feb 15, 2009 mloukianov Started adding session and application-based variables for account maintenance
 * Feb 26, 2009 mloukianov Adding session information to some logging
 * Mar 25, 2009 mloukianov Moved some stuff into ConnectionSession class
 * Jul 31, 2009 mloukianov Moved stuff out of ConnectionSession class
 * Oct 19, 2009 mloukianov Added CCITT CRC-16 support
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 * Apr 14, 2010 mloukianov Fixed bonus games calculations in SCRATCH mode
 * May 06, 2010 mloukianov Fixed an issue with terminal emergency count when player card is not inserted
 * Jul 03, 2010 mloukianov Added support for terminal and game state machines
 * Sep 15, 2010 mloukianov Hunting potential deadlock
 * Sep 16, 2010 mloukianov Added mechanism for putting all session-specific variables into single class
 * Sep 18, 2010 mloukianov Put everything for paper ticket purchase into single connection lookup
 * Apr 24, 2011 mloukianov Fixed bug with playing in scratch mode after terminal reboot
 * May 09, 2011 mloukianov Fixed GRUKR-872: complete bonus ticket if DEPO_REQ is sent while playing bonus games
 * Jul 08, 2011 mloukianov Fixed calculateBonusSpins method (defect GRUKR-973)
 * Jul 20, 2011 mloukianov Fixed GRUKR-973 one more time: added multiplication by number of lines played in calculateBonusSpins method
 * Aug 28, 2011 mloukianov Factored out special cards variables into FurorCardTracker
 * Oct 18, 2011 mloukianov Fixed send buyTicketResponse (write() -> writeInt())
 */
package com.ninelinelabs.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.net.Socket;

import java.sql.SQLException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ninelinelabs.authentication.vo.AuthenticationResult;
import com.ninelinelabs.game.slots.vo.LotteryGameResult;
import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.io.DataOutputStreamEx;

import com.ninelinelabs.lottery.generator.vo.bor.BorLongTicketSpinVO;
import com.ninelinelabs.lottery.generator.vo.bor.BorLongTicketVO;
import com.ninelinelabs.message.Message;
import com.ninelinelabs.message.handler.MessageHandler;
import com.ninelinelabs.message.handler.MessageHandlerRegistry;
import com.ninelinelabs.message.parser.HPSP20MessageParser;
import com.ninelinelabs.message.parser.MessageParser;
import com.ninelinelabs.message.response.Response;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;
import com.ninelinelabs.server.ConnectionSession.State;

import com.ninelinelabs.server.state.TerminalState;
import com.ninelinelabs.util.CCITTCRC16;

import com.ninelinelabs.server.furor.FurorCardTracker;

/**
 * A class representing HPSP protocol handler for lottery server.
 * Used with TCP/IP transport layer.
 *
 * ConnectionRunnable represents a session with the terminal. We will use it
 * to store various parameters and states for the state machine representing
 * terminal and particular game on the terminal.
 *
 * For example:
 * <pre>
 *  ServerSocket listener = new ServerSocket(port);
 *  ExecutorService pool = Executors.newFixedThreadPool(poolsize);
 *
 *	pool.execute(new ConnectionRunnable(listener.accept()));
 * </pre>
 *
 * <B>NOTE:</B> I've added session and application state variables that should be made accessible from
 * Web Services layer to produce necessary results for WS API, as well as make the whole application faster
 * and more flexible.
 * <P>
 * Here is the idea:
 * <BR>
 * 1. When new session is created, all temporary accounts are created, but in-memory only
 * 2. Transactions become primary source of information about game state and such
 * 3. Concept of transaction is expanded to include information about game state
 * 4. Transaction record contains updated balances for both accounts
 *    (not sure if this is needed, since we might get another contention state)
 * 5. When session is closed, transactions related to this session are replayed and account
 *    balances in the database updated accordingly.
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 283 $ $Date: 2011-10-18 08:58:02 -0500 (Tue, 18 Oct 2011) $
 * @see
 */
public class ConnectionRunnable implements Runnable {

	private static final Logger logger = Logger.getLogger(ConnectionRunnable.class.getName());

	private final Socket socket;
	private final SessionTracker tracker;
	private final MessageParser parser;
	private final MessageHandlerRegistry handlerRegistry;

	private String sessionId;

	private String terminal;
	private String game;
	private String cardno;
	private String lastpin;

	private volatile int bank = 0;
	private volatile int credits = 0;
	private volatile int win = 0;

	private int denomination = 0;

	private int statecheck = TerminalState.DEMO;

	private int terminalstate = TerminalState.DEMO;

	private String servicecard = "";

	private ConnectionSession.State state;

	// state of current ticket
	private BorLongTicketVO bonusticket;

	private BorLongTicketSpinVO bonusCurrentSpin;

	private BorLongTicketSpinVO lastSpin;

	private int choicebonusnum = 0;
	private int[] bonuses;
	private int nominaltotalbet = 0;

	// transient
	private int lastping = 0;
	private int[] lines;
	private int bet;
	private int totalLines;

	// utilized ticket support
	private volatile boolean utilized = true;

	private static final Map<String, String> lastpins = Collections.synchronizedMap(new HashMap<String, String>());

	// current game state
	private int gamestate = Game.GAME_NONE;

	private FurorCardTracker cardTracker;
	
	private volatile boolean cleanupStarted = false;
	
	private volatile boolean scratchmode = false;

	
	/**
	 * Creates ConnectionRunnable instance for processing a socket connected to a terminal.
	 *
	 * @param socket  Socket connected to terminal
	 */
	public ConnectionRunnable(final Socket socket) {
		this.socket  = socket;
		this.tracker = new SessionTracker(this);
		this.parser = new HPSP20MessageParser();
		this.handlerRegistry = new MessageHandlerRegistry();
	}


	/**
	 * Set terminal session state.
	 *
	 * @param state  new session state
	 */
	private synchronized void setState(ConnectionSession.State state) {

		// FIXME: this one should check for correct transition and log errors, if any
		// see if we could put the state machine in here; the sesison state is not persistent, 
		// so we can safely assume everything happens in memory

		this.state = state;

		if (sessionId != null)
			logger.log(Level.INFO, sessionId + ": session state is set to {0}", ConnectionSession.state_names[state.num()]);
	}


	/**
	 * Returns terminal session state.
	 */
	public synchronized ConnectionSession.State getState() {
		return state;
	}
	
	
	private void logMessageType(String msgtype) {
		
		if (!msgtype.equals(ProtocolHPSP20.HPSP_20_PING_REQ) && !msgtype.equals(ProtocolHPSP20.HPSP_20_TLOG_REQ)) {
			logger.log(Level.INFO, sessionId + " : received message " + msgtype);
		}
	
	} 
	
	
	private void processMessage(String msgtype, DataInputStreamEx dds, DataOutputStreamEx dos) throws IOException {
		
		if (msgtype.equals(ProtocolHPSP20.HPSP_20_PLAY_REQ)) {
			
			handlePlayRequest(dds, dos);

		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_DUBL_REQ)) {
			
			handleDoubleRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_BNUS_REQ)) {
			
			handleBonusRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_AUTH_REQ)) {
			
			handleAuthenticationRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_CASH_REQ)) {
			
			handleDepositRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_COUT_REQ)) {
			
			handleCashOutRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_DEPO_REQ)) {
			
			handleDepositBNARequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_TIKT_REQ)) {
			
			handleBuyTicketRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_EXIT_REQ)) {
			
			handleExitLotteryRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_TLOG_REQ)) {
			
			handleLogRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_PING_REQ)) {
			
			handlePingRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_COUNT_REQ)) {
			
			handleCountRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_ENDCT_REQ)) {
			
			handleEndCountRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_CONN_REQ)) {
			
			handleConnRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_RECN_REQ)) {
			
			handleReconnectRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_REEL_REQ)) {
			
			handleReelsRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_ROLL_END)) {
			
			handleRollEndRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_ROLL_ERR)) {
			
			handleRollErrRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_ROLL_TKT)) {
			
			handleRollTktRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_ROLL_INIT)) {
			
			handleRollInit(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_CARD_REQ)) {
			
			handleDealerCardRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_PARM_GET)) {
			
			handleParameterGetRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_PARM_PUT)) {
			
			handleParameterPutRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_PRNT_REQ)) {
			
			handlePrintRequest(dds, dos);
			
		} else if (msgtype.equals(ProtocolHPSP20.HPSP_20_PRNT_CNF)) {
			
			handlePrintConfirm(dds, dos);
			
		} else {
			
			logger.log(Level.SEVERE, sessionId + ": processing unknown message type: {0}", msgtype);
			handleUnknownMessage(dds, dos);
		}
		
	}
	
	
	private byte[] getMessageBytes(int msglen, DataInputStreamEx ds) throws IOException {
		
		int len = 0;
		
		byte[] msg = new byte[msglen + 2];

		do {
			
			len = len + ds.read(msg, len, (msglen + 2 - len));
			
		} while (len != (msglen + 2));

		return msg;
	}
	
	
	private int getMessageLength(DataInputStreamEx ds) throws IOException {
		
		return ds.readInt();
	}

	
	private boolean checkCRC(byte[] msg) {
		
		byte[] crc = CCITTCRC16.crc16(msg);

		return (crc[0] == 0x00 && crc[1] == 0x00);
	}
	
	
	private boolean sessionIsConnected() {
		
		return (state != ConnectionSession.State.DISCONNECTED_STATE);
	}


	/**
	 * Performs initial message processing and calls handler for specific message type.
	 * Implementation of run() method from Runnable interface.
	 *
	 * @see java.lang.Runnable
	 */
	@Override
	public void run() {

		try {

			DataInputStreamEx ds = new DataInputStreamEx(new BufferedInputStream(socket.getInputStream()));
			DataOutputStreamEx dos = new DataOutputStreamEx(socket.getOutputStream());

			setState(ConnectionSession.State.CONNECTED_STATE);

			tracker.start();

			while( sessionIsConnected() ) {

				int msglen = getMessageLength(ds);

				if (msglen == FlashUtils.MAGIC_NUMBER) {

					FlashUtils.sendPolicyFile(ds, dos);
					
					setState(ConnectionSession.State.DISCONNECTED_STATE);

				} else {
					
					byte[] msg = getMessageBytes(msglen, ds);

					logger.log(Level.FINE, "Performing CRC check on message received");
					
					if (checkCRC(msg)) {
						
						Message message = parser.parse(msg);
						
						logMessageType(message.getMsgtype());
						
						Response response = handlerRegistry.getHandler(message.getMsgtype()).handle(message);
						
						sendMessage(response.toByteArray(), dos);

					} else {
						
						logger.log(Level.SEVERE, "CRC check error");
					}
				}
			}
		} 
		catch (EOFException e) {

			if (terminal != null) {
				
				logger.log(Level.INFO, sessionId + " : exception when processing socket data", e);
				logger.log(Level.INFO, sessionId + " : connection to terminal {0} lost", terminal);

			} else {

				logger.log(Level.INFO, "EOFException: connection to terminal lost; terminal id is null", e);
			}

		} catch(IOException ioe) {

			if (terminal != null) {

				logger.log(Level.INFO, sessionId + " : connection to terminal {0} lost", terminal);
				logger.log(Level.INFO, sessionId + " : exact IO exception : " + ioe.getClass().getName());

			} else {

				logger.log(Level.INFO, "connection to terminal lost; terminal id is null", ioe);
			}

		} catch(Exception e) {

			logger.log(Level.INFO, sessionId + " : exception in socket listener run() method", e);

		} finally {

			tracker.stop();

			cleanupSession();
			
			closeSocket();
		}
	}


	private void completeBonusTicket() {

		if (utilized) {
			logger.log(Level.INFO, "{0} : Completing bonus games : this ticket is utilized; no bonus plays should be completed", sessionId);
			return;
		}

		if (bonusticket != null && bonusticket.isBonus()) {

			logger.log(Level.INFO, "{0} : Incomplete bonus games found for game {1}", new Object[] {sessionId, game} );

			BorLongTicketSpinVO[] bonusSpins = bonusCurrentSpin.getBonusSpins();

			while ((bonusSpins != null) && (bonusticket.getBonusPointer() < bonusSpins.length)) {

				BorLongTicketSpinVO bonusSpin = bonusSpins[bonusticket.getBonusPointer()];

				int linesPlayed = getLinesPlayed(lines);

				// Fix for defect "wrong calculations in Sharky" - changed function call from getTotalWin to getTotalWin3
				int totalWin = getTotalWin3(totalLines, lines, bet, bonusSpin);

				RequestProcessor.processPlayBonusLongTicketRequest(terminal, game, cardno, /* bet*linesPlayed */ 0, totalWin);

				this.win += totalWin;

				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: played one bonus spin on TICKET # " + bonusticket.getTicketNo() + ": bonus play # " + (bonusticket.getBonusPointer() + 1) + "\n" +
		                   sessionId + ":    BET = " + bet*linesPlayed + "; WIN = " + totalWin + "\n" +
		                   "; SPEC.SYM.=" + bonusSpin.getSpecialSymbol() + "; SP.SYM.WIN=" + bonusSpin.getSpecialSymbolWin());

				if (bonusSpin.getBonuses() != null) {
					for (int bonus : bonusSpin.getBonuses()) {

						int[] results = new int[1];

						results[0] = bonus * bet * linesPlayed;

						BonusRequestResult res = RequestProcessor.processBonusRequest(terminal, game, cardno, results);

						logger.log(Level.INFO, sessionId + ": played one bonus on ticket " + bonusticket.getTicketNo() + ": bonus amount " + results[0] +
								"; BANK = " + res.getBank() + "; CREDITS = " + res.getCredits() + "; WIN = " + res.getWin());
					}
				}

				bonusticket.moveBonusPointer();
			}
		}
	}
	
	
	private void logBankCreditsWin(long bank, long credit, long win) {
		
		logger.log(Level.INFO, sessionId + " : BANK = {0}, CREDIT = {1}, WIN = {2}", new Object[] {bank, credit, win});
	
	}

	
	/**
	 * Session cleanup when stale session is detected.
	 */
	public void cleanupSession() {
		
		if (cleanupStarted) return;
		
		cleanupStarted = true;

		logger.log(Level.INFO, "Session cleanup started; session state = " + getState().num() + "; session = " + sessionId);

		switch (getState().num()) {
			case 5:
			case 4:
			{
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: complete free games and bonuses: terminal = {0}, game = {1}", new Object[]{terminal, game});
				completeBonusTicket();
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: deposit WIN into CREDITS: terminal = {0}, game = {1}", new Object[]{terminal, game});
				RequestProcessor.processDepositRequest(terminal, game, this.bonusticket.getTicketNo(), (this.bonusticket.getPointer() + 2));
			}
			case 3:
			{
				String ticketno = bonusticket.getTicketNo();
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: deposit CREDITS into BANK; terminal = {0}, game = {1}, ticket={2}", new Object[]{terminal, game, ticketno});
				RequestProcessor.processExitLotteryRequest(terminal, game, cardno, ticketno, utilized);
				game = null;
			}
			case 2:
			{
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: deposit BANK into PIN; terminal = {0}, game = {1}", new Object[]{terminal, cardno});
				RequestProcessor.processCashoutRequest(terminal, cardno);
				RequestProcessor.addTLogRecord(terminal, 20002, "Connection to terminal " + terminal + " lost");

			}
			case 1:
			case 0:
			{
				RequestProcessor.performEmergencyCount(terminal);
				RequestProcessor.turnoffTerminal(terminal);

				RequestProcessor.recordStatusMessage(terminal, "terminal connection lost");

				terminal = null;
				cardno = null;

				setState(ConnectionSession.State.DISCONNECTED_STATE);
				sessionId = null;
			}
		}
	}


	private void closeSocket() {
		try { socket.close(); } catch (IOException e) {}
	}


	/**
	 * Returns last client ping time for this connection
	 *
	 * @return last ping time
	 */
	public int getLastPing() {
		return lastping;
	}


	/**
	 * Returns string containing session id.
	 * Format of the session id string:
	 * <code>SESSION:[terminal id]|[PIN or card number]|[system time in milliseconds session begun]</code>
	 *
	 * @return string containing session id
	 */
	public String getSessionId() {
		return sessionId;
	}


	/**
	 * Used to send back comm error message
	 */
	private void handleCommError(DataOutputStream dos) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_COMM_ERR);

		sendMessage(bos, dos, ddos);
	}


	/**
	 * Used to handle dealer card request for Igralot
	 */
	private void handleDealerCardRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		sendDealerCardResponse(dos, "");
	}


	private void sendDealerCardResponse(DataOutputStream dos, String card) throws IOException {

	}

	private void handleParameterGetRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String param = ds.readUTF();

		String value = RequestProcessor.getParam(terminal, param);

		sendParameterGetResponse(dos, terminal, value);
	}

	private void sendParameterGetResponse(DataOutputStream dos, String terminal, String value) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_PARM_RES);
		ddos.writeUTF(terminal);

		if (value != null) {
			ddos.writeUTF(value);
		} else {
			ddos.writeUTF("");
		}

		sendMessage(bos, dos, ddos);
	}

	private void handleParameterPutRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		int params = ds.readInt();

		String[] names = new String[params];
		String[] values = new String[params];

		for (int i = 0; i < params; i++) {
			names[i] = ds.readUTF();
			values[i] = ds.readUTF();
		}

		if (terminalstate != TerminalState.SERVICE) {
			// block terminal and record security event
		}

		RequestProcessor.putParams(terminal, names, values, servicecard);

		Denomination denomination = RequestProcessor.getTerminalDenomination(terminal);

		this.denomination = denomination.getDenomination();
	}


	private void handleRollInit(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		String rollid = ds.readUTF();

		logger.log(Level.INFO, sessionId + " : ROLL_INIT message received from terminal " + terminal + " with roll " + rollid);
		logger.log(Level.INFO, sessionId + " : Current service card number is : " + this.servicecard);

		boolean success = RequestProcessor.processRollInit(terminal, rollid, servicecard);

		if (success) {
			logger.log(Level.INFO, sessionId + " : Roll successfully initialized for roll id " + rollid);
		} else {
			logger.log(Level.SEVERE, sessionId + " : Roll init failed for roll id " + rollid);
		}

		sendRollInitResponse(dos, terminal, rollid, success);
	}


	private void sendRollInitResponse(DataOutputStream dos, String terminal, String rollid, boolean success) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_ROLL_RES);
		ddos.writeUTF(terminal);

		if (success) {
			ddos.writeUTF(rollid);
		} else {
			ddos.writeUTF("");
		}

		sendMessage(bos, dos, ddos);
	}

	private void handleRollEndRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		String rollid = ds.readUTF();

		logger.log(Level.INFO, sessionId + " : ROLL_END message received from terminal " + terminal + " for roll " + rollid);

		/*
		 * Roll end request puts terminal in "blocked" state.
		 * Blocked state is released when new roll is loaded into the terminal ()
		 */

		RequestProcessor.processRollEnd(terminal, rollid);

		sendRollEndResponse(dos, terminal, rollid);
	}

	private void sendRollEndResponse(DataOutputStream dos, String terminnal, String rollid) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_SHUT_RES);
		ddos.writeUTF(terminal);
		ddos.writeInt(12);

		sendMessage(bos, dos, ddos);
	}

	private void handleRollErrRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		String rollid = ds.readUTF();

		logger.log(Level.INFO, sessionId + " : ROLL_ERR message received from terminal " + terminal + " with roll " + rollid);

		RequestProcessor.processRollErr(terminal, rollid);

		sendRollErrResponse(dos, terminal, rollid);
	}

	private void sendRollErrResponse(DataOutputStream dos, String terminal, String roll) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_SHUT_RES);
		ddos.writeUTF(terminal);
		ddos.writeInt(15);

		sendMessage(bos, dos, ddos);
	}

	private void handleRollTktRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		String rollid = ds.readUTF();

		logger.log(Level.INFO, sessionId + " : ROLL_TKT request received from terminal " + terminal + " with roll " + rollid);

		RequestProcessor.processRollTktRequest(terminal, rollid);

		this.statecheck = TerminalState.TICKET_RESERVED;

		sendRollTiktResponse(dos, terminal, rollid);
	}


	private void sendRollTiktResponse(DataOutputStream dos, String terminal, String rollid) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_ROLL_READY);
		ddos.writeUTF(terminal);
		ddos.writeUTF(rollid);

		sendMessage(bos, dos, ddos);
	}

	private void handleReelsRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		String game = ds.readUTF();

		logger.log(Level.INFO, sessionId + " : REEL_REQ message received (game : {0})", game);

		byte[] reels = null;
		int percentage = 0;

		if (this.bonusticket == null) {
			percentage = RequestProcessor.getGamePercentage(terminal, game);
			reels = RequestProcessor.getGameReels(terminal, game);
		} else {
			percentage = this.bonusticket.getPercentage();
			reels = RequestProcessor.getGameReels(terminal, game, percentage);
		}

		sendReelsResponse(dos, terminal, game, percentage, reels);
	}


	private void sendReelsResponse(DataOutputStream dos, String terminal, String game, int percentage, byte[] reels) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_REEL_RES);
		ddos.writeUTF(terminal);
		ddos.writeUTF(game);
		ddos.writeInt(percentage);

		ddos.write(reels);

		sendMessage(bos, dos, ddos);
	}


	private void handleReconnectRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String clientsession = ds.readUTF();

		if (sessionId != null) return;

		logger.log(Level.INFO, sessionId + " : RECN_REQ message received ( client session : {0} )", clientsession);
		logger.log(Level.INFO, sessionId + " : Restoring client session on server...");

		restoreClientSession(clientsession);

		boolean reconnected = RequestProcessor.reconnectTerminal(terminal);

		if (!reconnected) {
			logger.log(Level.SEVERE, "Unsuccessful reconnect for terminal " + terminal + "; session = " + clientsession);
		}

		logger.log(Level.INFO, sessionId + " : Restored client session on server. Sending back reconnect response");

		sendReconnectResponse(dos, clientsession);
	}


	private void restoreClientSession(String session) {

		terminal = (String)SessionManager.getSessionManager().getParam(session, "terminal");
		cardno   = (String)SessionManager.getSessionManager().getParam(session, "cardno");
		game     = (String)SessionManager.getSessionManager().getParam(session, "game");
		lastpin  = (String)SessionManager.getSessionManager().getParam(session, "lastpin");

		logger.log(Level.INFO, "Restored session for terminal : " + terminal + "; game : " + game + "; cardno : " + cardno);

		this.sessionId = session;
	}


	/**
	 * Sends response after successful reconnect.
	 * Connection finds and restores all session values from the session that was disconnected.
	 *
	 * @param dos			data output stream
	 * @param session		session id (string)
	 */
	private void sendReconnectResponse(DataOutputStream dos, String session) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_RECN_RES);
		ddos.writeUTF(session);

		sendMessage(bos, dos, ddos);
	}



	/**
	 * Handle new terminal connection request.
	 * Uninitialized terminal will be initialized if needed.
	 * If terminal information is not found in the database, error response will be sent to the terminal.
	 *
	 * @param ds
	 * @param dos
	 * @throws IOException
	 */
	private void handleConnRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String card = ds.readUTF();
		String ticket = ds.readUTF();

		logger.log(Level.INFO, "CONN_REQ message received ( cardno : " + card + ", ticket: " + ticket + " )");

		TerminalRecord terminalRecord = RequestProcessor.connectTerminal(card, ticket);

		if (terminalRecord == null) {
			sendConnectErrorResponse(dos);
			return;
		}

		this.sessionId = terminalRecord.getHallname() + "|" + System.currentTimeMillis();

		this.terminal = terminalRecord.getHallname();
		this.denomination = terminalRecord.getDenomination();

		if (!ticket.equals(terminalRecord.getTicket())) {
			logger.log(Level.SEVERE, "Terminal " + terminal + ": mismatch with last printed ticket: terminal ticket: " + ticket + " != last printer terminal ticket: " + terminalRecord.getTicket());
		}

		sendConnectResponse(dos, terminalRecord.getHallname(), terminalRecord.getSerialno(), terminalRecord.getDenomination() + "", terminalRecord.getDescription(), sessionId,
																	terminalRecord.getTicket(), terminalRecord.getWin(), terminalRecord.getTimestamp(), terminalRecord.isPrint());
	}


	private void sendConnectErrorResponse(DataOutputStream dos) throws IOException {
		
		logger.log(Level.INFO, "Sending CONN_ERR from server to terminal");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_CONN_ERR);

		sendMessage(bos, dos, ddos);
	}


	private void sendConnectResponse(DataOutputStream dos, String terminal, String serialno, String assetid, String description, String session, String ticket, int win, int timestamp, boolean print) throws IOException {

		logger.log(Level.INFO, terminal + " : sending CONN_RES from server to terminal; serialno = " + serialno + "; assetid = " + assetid + "; description = " + description + "; session = " + session +
				"; ticket = " + ticket + "; win = " + win + "; timestamp = " + timestamp  + "; print required = " + print);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_CONN_RES);
		ddos.writeUTF(terminal);
		ddos.writeUTF(serialno);
		ddos.writeUTF(assetid);
		ddos.writeUTF(description);
		ddos.writeUTF(session);

		ddos.writeUTF(ticket);
		ddos.writeInt(win);
		ddos.writeInt(timestamp);
		ddos.writeInt(print?1:0);

		sendMessage(bos, dos, ddos);
	}


	public void stop() {

		logger.log(Level.INFO, "{0} : closing connection socket in ConnectionRunnable", sessionId);
		closeSocket();
	}


	private void handlePingRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminalid = ds.readUTF();
		int pingTime = ds.readInt();

		logger.log(Level.FINEST, sessionId + "{0} : PING_REQ message received from {1} : timestamp = {2}", new Object[]{sessionId, terminalid, new Integer(pingTime)} );

		// save the last ping time stamp
		lastping = pingTime;

		this.terminal = terminalid;

		boolean terminalBlocked = RequestProcessor.processPingRequest(terminalid);

		if (terminalBlocked) {
			sendBlockedResponse(dos, terminalid, pingTime);
		} else {
			sendPingResponse(dos, terminalid, pingTime);
		}
	}


	private void sendBlockedResponse(DataOutputStream dos, String terminal, int pingtime) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_SHUT_RES);
		ddos.writeUTF(terminal);
		ddos.writeInt(1);	// 1 - terminal temporary blocked until new day is started
		// FIXME document shutdown reason in protocol document

		sendMessage(bos, dos, ddos);
	}




	private void handleCountRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminalid = ds.readUTF();
		String cardNumber = ds.readUTF();
		String countMode = ds.readUTF();

		logger.log(Level.INFO, sessionId + "{0} : COUNT_REQ message received from terminal {1} : card {2}, count mode {3}", new Object[]{sessionId, terminalid, cardNumber, countMode} );

		BanknoteCount[] counts = new BanknoteCount[0];
		int total = 0;

		try {

			counts = RequestProcessor.performTerminalCount(terminalid, cardNumber, countMode);

			if (counts == null) {
				sendCountResponse(dos, terminalid, 0, new BanknoteCount[0] );
				return;
			}

			for (BanknoteCount count : counts) { total += count.getCount() * count.getDenomination(); }

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Unable to fetch terminal count information", e);
		}

		sendCountResponse(dos, terminalid, total, counts);
	}


	private void sendCountResponse(DataOutputStream dos, String terminalid, int total, BanknoteCount[] counts) throws IOException {

		logger.log(Level.INFO, terminal + " : sending COUNT_RES from server to terminal; total = " + total + "; # of bills = " + counts.length);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_COUNT_RES);
		ddos.writeUTF(terminalid);
		ddos.writeInt(total);
		ddos.writeInt(counts.length);

		for (BanknoteCount count : counts) {
			ddos.writeInt(count.getDenomination());
			ddos.writeInt(count.getCount());
		}

		sendMessage(bos, dos, ddos);
	}


	private void handleEndCountRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminalid = ds.readUTF();
		String cardno     = ds.readUTF();

		logger.log(Level.FINEST, "{0} : ENDCT_REQ message received from terminal {1} : card {2}", new Object[]{sessionId, terminalid, cardno} );

		try {
			RequestProcessor.recordEndCount(terminalid);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Unable to record terminal count", e);
		}

		sendEndCountResponse(dos, terminalid, cardno);
	}


	private void sendEndCountResponse(DataOutputStream dos, String terminalid, String cardno) throws IOException {

		logger.log(Level.INFO, terminal + " : sending ENDCT_RES from server to terminal");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_ENDCT_RES);
		ddos.writeUTF(terminalid);

		sendMessage(bos, dos, ddos);
	}


	private void sendPingResponse(DataOutputStream dos, String terminal, int pingtime) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_PING_RES);
		ddos.writeUTF(terminal);
		ddos.writeInt(pingtime);

		sendMessage(bos, dos, ddos);
	}


	private void handleLogRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		String terminal = ds.readUTF();
		int event = ds.readInt();
		String log = ds.readUTF();

		logger.log(Level.FINE, "{0} : TLOG_REQ message received from terminal {1} : event code = {2}, msg = {3}", new Object[]{sessionId, terminal, new Integer(event), log} );

		if (log.equals("BNA stacker removed")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				this.terminalstate = TerminalState.COUNT;
				// perform count based on current count type for this operday
				String counttype = RequestProcessor.getCountType(terminal);
				try {
					RequestProcessor.performTerminalCount(terminal, servicecard, counttype);
					RequestProcessor.recordEndCount(terminal);
				} catch (SQLException e) {
					logger.log(Level.SEVERE, sessionId + " : Failed to perform terminal count for terminal " + terminal, e);
				}
			} else {
				// raise security alert flag
				// record emergency count
				RequestProcessor.performEmergencyCount(terminal);
				RequestProcessor.recordStatusMessage(terminal, log);
			}
		} else if (log.equals("BNA stacker installed")) {
			if (this.terminalstate == TerminalState.COUNT) {
				this.terminalstate = TerminalState.SERVICE;
			} else {
				RequestProcessor.recordFraudAlert(terminal, log);
				// raise security alert flag
			}
		} else if (log.equals("main door open")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				// do nothing
			} else {
				// raise security alert flag
				RequestProcessor.recordFraudAlert(terminal, log);
				RequestProcessor.recordStatusMessage(terminal, log);
			}
		} else if (log.equals("stacker door open")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				// do nothing
			} else {
				// raise security alert flag
				RequestProcessor.recordFraudAlert(terminal, log);
				RequestProcessor.recordStatusMessage(terminal, log);
			}
		} else if (log.equals("main door close")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				// do nothing
			} else {
				// raise security alert flag
				RequestProcessor.recordFraudAlert(terminal, log);
			}
		} else if (log.equals("stacker door close")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				// do nothing
			} else {
				// raise security alert flag
				RequestProcessor.recordFraudAlert(terminal, log);
			}
		} else if (log.equals("BNA Communication error")) {
			logger.log(Level.INFO, sessionId + " : Received \"" + log + "\" event");
		} else if (log.equals("IO board error")) {
			logger.log(Level.INFO, sessionId + " : Received \"" + log + "\" event");
		} else if (log.equals("BNA general error")) {
			logger.log(Level.INFO, sessionId + " : Received \"" + log + "\" event");
		} else if (log.equals("card reader error")) {
			logger.log(Level.INFO, sessionId + " : Received \"" + log + "\" event");
		} else if (log.equals("card removed")) {
			if (this.terminalstate == TerminalState.SERVICE) {
				terminalstate = TerminalState.DEMO;
			}
		}

		RequestProcessor.addTLogRecord(terminal, event, log);
	}


	private void handlePrintRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String ticket = ds.readUTF();
		int win = ds.readInt();

		logger.log(Level.INFO, "{0} : PRNT_REQ message received from terminal {1}: paper ticket = {2}, ticket win = {3}",
				new Object[]{sessionId, terminal, ticket, new Integer(win)} );

		PrintRequestResult res = RequestProcessor.processPrintRequest(terminal, ticket, win);

		if (!res.success()) {
			logger.log(Level.SEVERE, "SLK can not process print ticket message");
			throw new IOException("Cannot process print ticket message");
		}

		sendPrintTicketResponse(dos, terminal, ticket, res.getTimestamp(), res.getWin());
	}


	private void sendPrintTicketResponse(DataOutputStream dos, String terminal, String ticket, long timestamp, long win) throws IOException {

		logger.log(Level.INFO, terminal + " : sending PRNT_RES from server to terminal; terminal = " + terminal + "; ticket = " + ticket + 
				"; timestamp = " + timestamp + "; win = " + win);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_PRNT_RES);
		ddos.writeUTF(terminal);

		ddos.writeUTF(ticket);
		ddos.writeLong(timestamp);
		ddos.writeLong(win);

		sendMessage(bos, dos, ddos);
	}


	private void handlePrintConfirm(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String ticket = ds.readUTF();
		int win = ds.readInt();

		logger.log(Level.INFO, "{0} : PRNT_CNF message received from terminal {1}: paper ticket = {2}, ticket win = {3}",
				new Object[]{sessionId, terminal, ticket, new Integer(win)} );

		boolean success = RequestProcessor.processPrintConfirm(terminal, ticket, win);

		if (!success) {
			throw new IOException("Cannot process print confirm message");
		}
	}


	private void handleBuyTicketRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal    = ds.readUTF();		// terminal id
		game               = ds.readUTF();		// game id
		int price          = ds.readInt();		// ticket price
		String paperticket = ds.readUTF();		// paper ticket number; UA-specific!

		logger.log(Level.INFO, "{0} : TIKT_REQ message received from terminal {1}: game = {2}, ticket price = {3}, BB = {4}",
							new Object[]{sessionId, terminal, game, new Integer(price), paperticket} );

		if (GameRegistry.getInstance().getGametype(game).equals(ProtocolHPSP20.LONGTICKETBONUS)) {

			this.utilized = true;

			String lottery = GameRegistry.getInstance().getLotteries().get(game);

			BuyTicketResult result = RequestProcessor.processBuyLongBonusPaperTicketRequest(terminal, cardno, game, lottery, price, paperticket);	// UA-specific

			if (result == null) {
				sendNoMoreTicketsResponse(dos, terminal, game);

				GameAvailability[] games = GameRegistry.getInstance().getGameAvailability(terminal);

				sendGameAvailability(terminal, games, dos);

				return;
			}

			if (result.isDoublesale()) {
				sendTicketAlreadySold(dos, terminal, paperticket);
				return;
			}

			bonusticket = result.getTicket();	// UA-specific

			SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "bonusticket", this.bonusticket);

			logger.log(Level.INFO, "    BANK balance = " + result.getBank() + "\n" +
					               "    CREDITS balance = " + result.getCredits());

			this.bank = result.getBank();
			this.credits = result.getCredits();

			this.statecheck = TerminalState.TICKET_PURCHASED;
			
			sendBuyTicketResponse(dos, terminal, game, result.getBank(), result.getCredits(), bonusticket.getTicketNo(), bonusticket.size(), 0);

		} else if (GameRegistry.getInstance().getGametype(game).equals(ProtocolHPSP20.IGRASOFT)) {

			// int bank = RequestProcessor.processBuyIgrasoftTicketRequest(terminal, game, amount);

			int bank = 0;

			logger.log(Level.INFO, "    BANK balance = " + bank + "\n" +
		               "    CREDITS balance = " + price);

			sendBuyTicketResponse(dos, terminal, game, bank, price, "", 0, 0);
		}

		setState(ConnectionSession.State.GAME_STATE);
	}


	private void sendMsg(DataOutputStream dos, int length, byte[] msg, byte[] crc) throws IOException {
		dos.writeInt(length);
		dos.write(msg);
		dos.write(crc);
		dos.flush();
	}


	private void sendTicketAlreadySold(DataOutputStream dos, String terminal, String paperticket) throws IOException {

		logger.log(Level.INFO, terminal + " : sending TIKT_ERR from server to terminal; terminal = " + terminal + "; ticket # = " + paperticket);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_TIKT_ERR);
		ddos.writeUTF(terminal);
		ddos.writeUTF(paperticket);

		sendMessage(bos, dos, ddos);
	}



	private void sendBuyTicketResponse(DataOutputStream dos, String terminal, String game, int bank, int credit, String ticket, int segmentscount, int reelsetno) throws IOException {

		logger.log(Level.INFO, terminal + " : sending TIKT_RES from server to terminal; game = " + game + "; BANK = " + bank + "; CREDIT = " + credit +
																								"; ticket # " + ticket + "; # of segments = " + segmentscount);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_TIKT_RES);
		ddos.writeUTF(terminal);

		// added/replaced to support BANK/CREDIT/WIN reqs
		ddos.writeInt(bank);	// BANK
		ddos.writeInt(credit);	// CREDIT
		ddos.writeInt(0);		// WIN

		ddos.writeUTF(game);
		// Added 03.24.09: ticket number (UTF)
		ddos.writeUTF(ticket);
		// Added 01.16.2010: number of segments in the ticket
		ddos.writeInt(segmentscount);
		// Added 03/01/2010: reelset number
		ddos.writeInt(reelsetno);

		sendMessage(bos, dos, ddos);
	}


	private void sendNoMoreTicketsResponse(DataOutputStream dos, String terminal, String game) throws IOException {

		logger.log(Level.INFO, terminal + " : sending ENDL_RES from server to terminal; game = " + game);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_ENDL_RES);
		ddos.writeUTF(terminal);
		ddos.writeUTF(game);

		sendMessage(bos, dos, ddos);
	}


	private void handleExitLotteryRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		if (getState().num() == State.LOBBY_STATE.num() ||
			getState().num() == State.CONNECTED_STATE.num() ||
			getState().num() == State.DISCONNECTED_STATE.num()) return;

		String terminal = ds.readUTF();
		String game     = ds.readUTF();

		logger.log(Level.INFO, sessionId + ": EXIT_REQ message received from " + terminal + ": game = " + game);

		String ticketno = null;

		if (game == null || game.equals("")) {
			logger.log(Level.SEVERE, sessionId + " : EXIT_REQ is missing game identifier; substituting game identifier from local session info: " + this.game);
			game = this.game;
		}


		if (GameRegistry.getInstance().getGametype(game).equals(ProtocolHPSP20.LONGTICKETBONUS)) {
			if (this.bonusticket == null) {
				ticketno = "";
			} else {
				ticketno = this.bonusticket.getTicketNo();
			}
		}

		int bank = RequestProcessor.processExitLotteryRequest(terminal, game, cardno, ticketno, utilized);

		this.credits = 0;
		this.bank = bank;

		this.bonusCurrentSpin = null;
		this.bonusticket = null;
		this.game = null;

		logger.log(Level.INFO, "    BANK balance = " + bank);

		this.statecheck = TerminalState.LOBBY;

		sendExitLotteryResponse(dos, terminal, game, bank);

		GameAvailability[] games = GameRegistry.getInstance().getGameAvailability(terminal);

		sendGameAvailability(terminal, games, dos);

		setState(ConnectionSession.State.LOBBY_STATE);
	}
	
	
	private void sendMessage(byte[] msg, DataOutputStream dos) throws IOException {
		
		byte[] crc = CCITTCRC16.crc16(msg);
		sendMsg(dos, msg.length, msg, crc);
	}


	private void sendMessage(ByteArrayOutputStream bos, DataOutputStream dos, DataOutputStream ddos) throws IOException {
		ddos.flush();
		byte[] msg = bos.toByteArray();
		byte[] crc = CCITTCRC16.crc16(msg);
		sendMsg(dos, msg.length, msg, crc);
		ddos.close();
		bos.close();
	}


	private void sendExitLotteryResponse(DataOutputStream dos, String terminal, String game, int bank)  throws IOException {

		logger.log(Level.INFO, terminal + " : sending EXIT_RES from server to terminal; game = " + game + "; BANK = " + bank + "; CREDIT = 0; WIN = 0");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_EXIT_RES);
		ddos.writeUTF(terminal);

		// added/modified to support BANK/CREDIT/WIN requirement
		ddos.writeInt(bank);	// BANK
		ddos.writeInt(0);		// CREDIT
		ddos.writeInt(0);		// WIN

		ddos.writeUTF(game);
		// ddos.writeInt(bank);

		sendMessage(bos, dos, ddos);
	}


	private void handlePlayRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal      = ds.readUTF();
		game                 = ds.readUTF();
		int nextBetSeq       = ds.readInt();
		bet                  = ds.readInt();
		totalLines           = ds.readInt();
		lines                = ds.readIntArray(totalLines);
		int totalbet         = ds.readInt();
		String visualization = ds.readUTF();

		if (cardTracker.getSpecialCondition() && cardTracker.stopPlay()) {
			cardTracker.resetSpecialCondition();
		}

		String gameclass = GameRegistry.getInstance().getGametype(game);

		this.utilized = false;

		if (gameclass.equals(ProtocolHPSP20.LONGTICKETBONUS)) {

			this.statecheck = TerminalState.SEG_REGULAR_PLAY;

			// get the ticket from local storage
			BorLongTicketVO vo = this.bonusticket;

			// Bonus game - no special case handling; but might have to adjust
			// lines and bet per line params
			if (vo.isBonus() && (totalbet == 0)) {
				
				this.scratchmode = false;

				if (cardTracker.getSpecialCondition()) {

					bet = cardTracker.getBet();
					totalLines = cardTracker.getTotalLines();
					lines = cardTracker.getLines();
					totalbet = cardTracker.getTotalBet();

				}

				BorLongTicketSpinVO[] bonusSpins = bonusCurrentSpin.getBonusSpins();

				if ((bonusSpins != null) && (vo.getBonusPointer() < bonusSpins.length)) {

					BorLongTicketSpinVO bonusSpin = bonusSpins[vo.getBonusPointer()];

					this.lastSpin = bonusSpin;

					this.choicebonusnum = 0;
					this.bonuses = bonusSpin.getBonuses();

					int linesPlayed = getLinesPlayed(lines);
					int totalWin = getTotalWin2(totalLines, lines, bet, bonusSpin);

					this.nominaltotalbet = linesPlayed * bet;

					PlayRequestResult res = RequestProcessor.processPlayBonusLongTicketRequest(terminal, game, cardno, /*bet*linesPlayed*/ 0, totalWin);

					// bonus spin; bet == 0
					if (!res.hasEnoughCreditsToPlay()) {
						throw new IOException("PLAY request can not be processed; most likely not enough credits to play");
					}

					cardTracker.incrementWin(totalWin);

					this.win += totalWin;

					logger.log(Level.INFO, sessionId + ": played one bonus spin on TICKET # " + bonusticket.getTicketNo() + ": regular play # " + (bonusCurrentSpin.getPointer() + 1) + "\n" +
		                       sessionId + ":    BET = " + bet*linesPlayed + "; WIN = " + totalWin + "\n" +
		                       "; SPEC.SYM.=" + bonusSpin.getSpecialSymbol() + "; SP.SYM.WIN=" + bonusSpin.getSpecialSymbolWin());

					sendPlayResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, nextBetSeq, totalWin, bonusSpin.getStops(), bonusSpin.getFreeGamesNumber(),
											bonusSpin.getScatter(), bonusSpin.getLines(), (vo.getBonusPointer() + 2), bonusSpin.getSpecialSymbol(),
																								bonusSpin.getSpecialSymbolWin(), (vo.getBonusPointer()+1), bonusSpin.getReelsetno());

					vo.moveBonusPointer();

				} else {

					vo.setBonus(false);
					vo.resetBonusPointer();
				}

			} else {
				// this is a regular game

				int pointer = vo.getPointer();

				if (pointer >= vo.size()) {
					// ticket has ended; send back TIKT_END message
					sendEndTicketResponse(dos, terminal, game);
					
					this.scratchmode = false;


				} else if (visualization.equals(ProtocolHPSP20.SCRATCH)) {
					// if this one is followed by bonus spins, calculate total bonus win and move it to escrow

					logger.log(Level.INFO, "Playing ticket in SCRATCH mode");
					
					this.scratchmode = true;

					BorLongTicketSpinVO spin = vo.getSpin(totalLines, bet, pointer);

					this.bonusCurrentSpin = spin;

					// FIX for GRUKR-861 (no bonus in all SCRATCH games)
					this.lastSpin = spin;

					vo.movePointer();

					if (cardTracker.getSpecialCondition() && pointer < (vo.size()-1)) {

						SpecialCardHandler handler = new SpecialCardHandler();
						SpecialCardWin specialwin = handler.findClosestWin(FurorCardTracker.MAX_SPECIAL/denomination, cardTracker.getCumulativeWin(), spin);

						logger.log(Level.INFO, "Found closest special card win: lines = " + specialwin.getLines() + "; bet = " + specialwin.getBet() + "; win = " + specialwin.getWin());

						// set bet parameters and save them in special variables

						cardTracker.setBet(specialwin.getBet());
						cardTracker.setTotalLines(specialwin.getLines());

						// set given number of lines (previously set in setTotalLines) to in-play
						cardTracker.resetLines();

						cardTracker.setTotalBet(cardTracker.getBet()*cardTracker.getTotalLines());

						if (cardTracker.getTotalBet() > this.credits) {
							// reduce total bet so that it fits into this.credits

							int temp_special_bet = this.credits / cardTracker.getTotalLines();

							logger.log(Level.INFO, "New special bet: " + temp_special_bet + "; formal special bet: " + cardTracker.getTotalBet());

							cardTracker.setBet(temp_special_bet);
							cardTracker.setTotalBet(cardTracker.getBet() * cardTracker.getTotalLines());
						}

						bet = cardTracker.getBet();
						totalLines = cardTracker.getTotalLines();
						lines = cardTracker.getLines();
						totalbet = cardTracker.getTotalBet();
					}

					if (spin.getFreeGamesNumber() > 0 && spin.getBonusSpins() != null) {

						this.lastSpin = spin.getBonusSpins()[spin.getBonusSpins().length - 1];
					}


					int linesPlayed = getLinesPlayed(lines);

					// 1. calculate regular win (as usual)

					int totalWin = getTotalWin(totalLines, lines, bet, spin);

					logger.log(Level.INFO, "Total win in SCRATCH mode: " + totalWin);

					int totalTotalBonusWin = 0;


					if (spin.getFreeGamesNumber() > 0) {

						// if free games are present for this regular spin

						logger.log(Level.INFO, "number of free games in SCRATCH mode: " + spin.getFreeGamesNumber());

						vo.setBonus(true);
						vo.resetBonusPointer();

						totalTotalBonusWin = calculateBonusSpins(spin, bet, lines);
					}

					PlayRequestResult res = RequestProcessor.processPlayLongTicketRequest(terminal, game, cardno, vo.getTicketNo(), visualization, bet, linesPlayed, (totalWin + totalTotalBonusWin), (vo.getPointer() + 2));

					if (!res.hasEnoughCreditsToPlay()) {
						throw new IOException("PLAY request can not be processed; most likely not enough credits to play or some other problem with transaction processing");
					}

					logger.log(Level.INFO, sessionId + ": played one regular spin on TICKET # " + bonusticket.getTicketNo() + ": regular play # " + (pointer + 1) + "\n" +
					                       sessionId + ":    BET = " + bet*linesPlayed + "; WIN = " + (totalWin + totalTotalBonusWin) + "\n" +
					                       "; SPEC.SYM.=" + spin.getSpecialSymbol() + "; SP.SYM.WIN=" + spin.getSpecialSymbolWin() + "; TOT.BON.WIN=" + totalTotalBonusWin);

					cardTracker.decrementBet(bet * linesPlayed);
					cardTracker.incrementWin(totalWin + totalTotalBonusWin);


					this.win += (totalWin + totalTotalBonusWin);
					this.credits -= bet*linesPlayed;

					sendPlayResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, nextBetSeq, (totalWin + totalTotalBonusWin), spin.getStops(), spin.getFreeGamesNumber(),
											spin.getScatter(), spin.getLines(), (pointer + 2), spin.getSpecialSymbol(),
											spin.getSpecialSymbolWin(), totalTotalBonusWin, spin.getReelsetno());
				} else {
					
					this.scratchmode = false;

					BorLongTicketSpinVO spin = vo.getSpin(totalLines, bet, pointer);
					this.bonusCurrentSpin = spin;

					this.lastSpin = spin;

					vo.movePointer();

					if (cardTracker.getSpecialCondition() && pointer < (vo.size()-1)) {

						SpecialCardHandler handler = new SpecialCardHandler();
						SpecialCardWin specialwin = handler.findClosestWin(FurorCardTracker.MAX_SPECIAL/denomination, cardTracker.getCumulativeWin(), spin);

						logger.log(Level.INFO, "Found closest special card win: lines = " + specialwin.getLines() + "; bet = " + specialwin.getBet() + "; win = " + specialwin.getWin());

						// set bet parameters and save them in special variables

						cardTracker.setBet(specialwin.getBet());
						cardTracker.setTotalLines(specialwin.getLines());

						cardTracker.resetLines();

						cardTracker.setTotalBet(cardTracker.getBet() * cardTracker.getTotalLines());

						if (cardTracker.getTotalBet() > this.credits) {
							// reduce total bet so that it fits into this.credits

							int temp_special_bet = this.credits / cardTracker.getTotalLines();

							logger.log(Level.INFO, "New special bet: " + temp_special_bet + "; formal special bet: " + cardTracker.getTotalBet());

							cardTracker.setBet(temp_special_bet);
							cardTracker.setTotalBet(cardTracker.getBet() * cardTracker.getTotalLines());
						}

						bet = cardTracker.getBet();
						totalLines = cardTracker.getTotalLines();
						lines = cardTracker.getLines();
						totalbet = cardTracker.getTotalBet();
					}

					this.choicebonusnum = 0;
					this.bonuses = spin.getBonuses();

					int linesPlayed = getLinesPlayed(lines);

					int totalWin = getTotalWin(totalLines, lines, bet, spin);

					this.nominaltotalbet = linesPlayed * bet;

					int totalTotalBonusWin = 0;

					if (spin.getFreeGamesNumber() > 0) {

						vo.setBonus(true);
						vo.resetBonusPointer();

						totalTotalBonusWin = calculateBonusSpins(spin, bet, lines);
					}

					PlayRequestResult res = RequestProcessor.processPlayLongTicketRequest(terminal, game, cardno, vo.getTicketNo(), visualization, bet, linesPlayed, totalWin, (vo.getPointer() + 2));

					if (!res.hasEnoughCreditsToPlay()) {
						throw new IOException("PLAY request can not be processed; most likely not enough credits to play");
					}

					this.win += totalWin;
					this.credits -= bet * linesPlayed;

					cardTracker.decrementBet(bet * linesPlayed);
					cardTracker.incrementWin(totalWin);

					logger.log(Level.INFO, sessionId + ": played one regular spin on TICKET # " + bonusticket.getTicketNo() + ": regular play # " + (pointer + 1) + "\n" +
					                       sessionId + ":    BET = " + bet*linesPlayed + "; WIN = " + totalWin + "\n" +
					                       "; SPEC.SYM.=" + spin.getSpecialSymbol() + "; SP.SYM.WIN=" + spin.getSpecialSymbolWin() + "; TOT.BON.WIN=" + totalTotalBonusWin);

					sendPlayResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, nextBetSeq, totalWin, spin.getStops(), spin.getFreeGamesNumber(),
											spin.getScatter(), spin.getLines(), (pointer + 2), spin.getSpecialSymbol(),
											spin.getSpecialSymbolWin(), totalTotalBonusWin, spin.getReelsetno());

					// if this segment does not have free games
					if (cardTracker.getSpecialCondition() && pointer < (vo.size()-1) && spin.getFreeGamesNumber() == 0) {

						// while we have more double-ups and we are not above high watermark
						while (lastSpin.getDoubleup() > 0 && (cardTracker.getCumulativeWin() + totalWin) <= cardTracker.highWatermark()) {

							lastSpin.useDoubleup();
							RequestProcessor.processPlayLongTicketDoubleupRequest(terminal, game, cardno, totalWin, totalWin*2, this.bonusticket.getTicketNo(), (this.bonusticket.getPointer() + 2));

							cardTracker.incrementWin(totalWin*2);
							cardTracker.decrementBet(totalWin);

							totalWin = totalWin*2;
						}
					}
				}
			}

		}

		setState(ConnectionSession.State.WIN_STATE);
	}

	private int getTotalWin3(int totalLines, int[] lines, int bet, BorLongTicketSpinVO spin) {

		int totalWin = 0;
		int linesPlayed = 0;

		for (int i = 0; i < totalLines; i++) {
			totalWin += spin.getLines()[i]*lines[i]*bet;
			linesPlayed += lines[i];
		}

		totalWin += linesPlayed*bet*spin.getScatter();

		return totalWin;
	}


	private int getTotalWin(int totalLines, int[] lines, int bet, BorLongTicketSpinVO spin) {

		int totalWin = 0;
		int linesPlayed = 0;

		for (int i = 0; i < totalLines; i++) {
			totalWin += spin.getLines()[i]*lines[i]*bet;
			linesPlayed += lines[i];
		}

		totalWin += linesPlayed*bet*spin.getScatter();

		for (int bonus : spin.getBonuses()) {
			totalWin += bonus*bet*linesPlayed;
		}

		return totalWin;
	}


	private int getTotalWin2(int totalLines, int[] lines, int bet, BorLongTicketSpinVO spin) {

		int totalWin = 0;
		int linesPlayed = 0;

		for (int i = 0; i < totalLines; i++) {
			totalWin += spin.getLines()[i]*lines[i]*bet;
			linesPlayed += lines[i];
		}

		totalWin += linesPlayed*bet*spin.getScatter();

		return totalWin;
	}


	private int getLinesPlayed(int[] lines) {

		int linesPlayed = 0;

		for (int line : lines) {
			linesPlayed += line;
		}

		return linesPlayed;
	}


	/**
	 * Calculate total win for all bonus spins associated with given spin
	 *
	 * @param spin	current spin
	 * @param game	game identifier
	 * @param bet	bet amount
	 * @param lines	array containing lines played
	 *
	 * @return  total bonus win recursively calculated for all bonus spins down form current spin
	 */
	private int calculateBonusSpins(BorLongTicketSpinVO spin, int bet, int[] lines) {

		int linesPlayed = 0;

		// calculate number of lines played
		for (int i = 0; i < lines.length; i++) {
			linesPlayed += lines[i];
		}

		int resultWin = 0;

		if (spin.getFreeGamesNumber() > 0) {

			// for each bonus spin
			// Fix for GRUKR-973:
			// for (BorLongTicketSpinVO bonusSpin : bonusCurrentSpin.getBonusSpins()) {
			for (BorLongTicketSpinVO bonusSpin : spin.getBonusSpins()) {

				int totalBonusWin = 0;

				// calculate sum of line wins for this spin
				for (int i = 0; i < lines.length; i++) { totalBonusWin += bonusSpin.getLines()[i]*lines[i]*bet; }

				// add scatter win amount
				totalBonusWin += linesPlayed*bet*bonusSpin.getScatter();

				// add choice bonus wins for this bonus play
				for (int bonus : bonusSpin.getBonuses()) {
					// Fix for GRUKR-973: wrong content in TOTAL.BONUS.WIN in last regular spin result:
					// added multiplication by number of lines played
					totalBonusWin += bonus*bet*linesPlayed;
				}

				resultWin += totalBonusWin;

				// calculate bonus win if this spin added more bonus games
				if (bonusSpin.getBonusSpins() != null) {
					resultWin += calculateBonusSpins(bonusSpin, bet, lines);
				}
			}
		}

		return resultWin;
	}


	private void sendPlayResponse(DataOutputStream dos, String terminal, long bank, long credits, long win, String game, int nextBetSeq, int linewin,
			int[] stops, int bonuses, int scatter, int winPerLine[], int cell, int specialSymbol, int specialSymbolWin, int currentFreeGameNum, int reelsetno) throws IOException {

		logger.log(Level.INFO, terminal + " : sending PLAY_RES from server to terminal; game = " + game + "; WIN = " + win);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_PLAY_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		ddos.writeLong(bank);
		ddos.writeLong(credits);
		ddos.writeLong(win);

		ddos.writeUTF(game);							    // game id - UTF
		ddos.writeInt(nextBetSeq+1);					// next bet # - int

		for (int j = 0; j < stops.length; j++) {
			ddos.writeInt(stops[j]);			       // 5 stops - short
		}

		for (int i = 0; i < winPerLine.length; i++ ) {
			ddos.writeInt(winPerLine[i]);				// win on particular lines (if played or not played)
		}

		ddos.writeInt(linewin);					          // win amount - Int

		ddos.writeInt(bonuses);							// number of bonus rounds (free games, etc)

		ddos.writeInt(scatter);							// scatter win

		// Added 03.23.09: cell number (int)
		ddos.writeInt(cell);

		ddos.writeInt(specialSymbol);
		ddos.writeInt(specialSymbolWin);

		ddos.writeInt(currentFreeGameNum);

		ddos.writeInt(reelsetno);

		sendMessage(bos, dos, ddos);
	}


	private void sendEndTicketResponse(DataOutputStream dos, String terminal, String game) throws IOException {

		logger.log(Level.INFO, terminal + " : sending TIKT_END from server to terminal; game = " + game);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_TIKT_END);		// message ID - UTF
		ddos.writeUTF(terminal);						    // terminal id - UTF
		ddos.writeUTF(game);							    // game id - UTF

		this.sendMessage(bos, dos, ddos);
	}


	private void handleBonusRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String game = ds.readUTF();

		int num = ds.readInt();

		int[] params = new int[num];

		for (int i = 0; i < num; i++) {
			params[i] = ds.readInt();
		}

		logger.log(Level.INFO, sessionId + ": BNUS_REQ message received from " + terminal + ": GAME = " + game + "\n" +
                                                                            sessionId + ": bonus NUM = " + num );

		String gameclass = GameRegistry.getInstance().getGametype(game);

		if (gameclass.equals(ProtocolHPSP20.LONGTICKET)) {

			logger.log(Level.SEVERE, "LONGTICKET game class does not have bonuses");

		} else if (gameclass.equals(ProtocolHPSP20.LONGTICKETBONUS)) {

			if ((choicebonusnum + 1) > bonuses.length) {
				// bonus count exceeded
				logger.log(Level.SEVERE, "Bonus count exceeded for game " + game);

				sendEndBonusResponse(dos, terminal, game);
			} else {

				this.statecheck = TerminalState.SEG_BONUS_PLAY;

				int[] results = new int[1];

				results[0] = bonuses[choicebonusnum++] * nominaltotalbet;

				BonusRequestResult res = RequestProcessor.processBonusRequest(terminal, game, cardno, results);

				if (!res.success()) {
					throw new IOException("Unable to process double-up request");
				}

				logger.log(Level.INFO, sessionId + ": played one bonus on ticket " + bonusticket.getTicketNo() + ": bonus amount " + results[0] +
						"; BANK = " + res.getBank() + "; CREDITS = " + res.getCredits() + "; WIN = " + res.getWin());

				this.win += results[0];

				cardTracker.incrementWin(results[0]);

				sendBonusResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, results);
			}

		} else if (gameclass.equals(ProtocolHPSP20.SLOT)) {

		} else if (gameclass.equals(ProtocolHPSP20.SHORTTICKET)) {

		} else {

			logger.log(Level.SEVERE, "Unknown game class: " + gameclass);
		}

	}


	private void sendEndBonusResponse(DataOutputStream dos, String terminal, String game) throws IOException {

		logger.log(Level.INFO, terminal + " : sending BNUS_END from server to terminal; game = " + game);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_BNUS_END);		// message ID - UTF
		ddos.writeUTF(terminal);						    // terminal id - UTF
		ddos.writeUTF(game);							    // game id - UTF

		sendMessage(bos, dos, ddos);
	}


	private void sendBonusResponse(DataOutputStream dos, String terminal, long bank, long credits, long win, String game, int[] bonuses) throws IOException {

		logger.log(Level.INFO, terminal + " : sending BNUS_RES from server to terminal; game = " + game + "; bonus win = " + bonuses[0]);


		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_BNUS_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						    // terminal id - UTF

		ddos.writeLong(bank);
		ddos.writeLong(credits);
		ddos.writeLong(win);

		ddos.writeUTF(game);							    // game id - UTF

		ddos.writeInt(bonuses.length);

		for (int bonus : bonuses) {
			ddos.writeInt(bonus);
		}

		this.sendMessage(bos, dos, ddos);
	}


	private void handleDoubleRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String game = ds.readUTF();
		int nextBetSeq = ds.readInt();
		int bet = ds.readInt();
		int color = ds.readInt();

		logger.log(Level.INFO, sessionId + ": DUBL_REQ message received from " + terminal + ": GAME = " + game + "\n" +
		                       sessionId + ":    BET = " + bet + "; DENOM = 1");

		if (GameRegistry.getInstance().getGametype(game).equals(ProtocolHPSP20.LONGTICKETBONUS)) {

			this.win -= bet;

			this.statecheck = TerminalState.SEG_DOUBLE_PLAY;

			if (lastSpin.getDoubleup() > 0) {

				lastSpin.useDoubleup();

				DoubleupRequestResult res = RequestProcessor.processPlayLongTicketDoubleupRequest(terminal, game, cardno, bet, bet*2, this.bonusticket.getTicketNo(), (this.bonusticket.getPointer() + 2));

				if (!res.success()) {
					logger.log(Level.SEVERE, "Problem processing double-up request");
					throw new IOException("Problem processing double-up request");
				}

				sendDoubleResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, nextBetSeq++, bet*2, this.lastSpin.getDoubleup(), color);

				cardTracker.incrementWin(bet*2);

				this.win += bet*2;

				logger.log(Level.INFO, sessionId + ": DUBL_RES message sent to " + terminal + ": GAME = " + game + "\n" +
						               sessionId + ":    BET = " + bet + "; DENOM = 1; WIN = " + bet*2);
			} else {

				DoubleupRequestResult res = RequestProcessor.processPlayLongTicketDoubleupRequest(terminal, game, cardno, bet, 0, this.bonusticket.getTicketNo(), (this.bonusticket.getPointer() + 2));

				if (!res.success()) {
					logger.log(Level.SEVERE, "Problem processing double-up request");
					throw new IOException("Problem processing double-up request");
				}

				sendDoubleResponse(dos, terminal, res.getBank(), res.getCredits(), res.getWin(), game, nextBetSeq++, 0, 0, color);

				logger.log(Level.INFO, sessionId + ": DUBL_RES message sent to " + terminal + ": GAME = " + game + "\n" +
				                       sessionId + ":    BET = " + bet + "; DENOM = 1; WIN = " + 0);
			}

		} else if (GameRegistry.getInstance().getGametype(game).equals(ProtocolHPSP20.IGRASOFT)) {

		}
	}


	private void sendDoubleResponse(DataOutputStream dos, String terminal, long bank, long credits, long totalwin, String game, int nextBetSeq, int win, int next, int color) throws IOException {

		logger.log(Level.INFO, terminal + " : sending DUBL_RES from server to terminal; game = " + game + "; win = " + win + "; next = " + next + "; color = " + color);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_DUBL_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		ddos.writeLong(bank);
		ddos.writeLong(credits);
		ddos.writeLong(totalwin);

		ddos.writeUTF(game);							    // game id - UTF
		ddos.writeInt(nextBetSeq+1);					// next bet # - int

		ddos.writeInt(win);		// win amount - either 0 or double the bet amount
		ddos.writeInt(next);		// 0 if no more double allowed; not null if more double allowed
		ddos.writeInt(color);	// returning the same color back to the client

		this.sendMessage(bos, dos, ddos);
	}


	private void handleAuthenticationRequest(DataInputStreamEx ds, DataOutputStreamEx dos) throws IOException {

		String terminal = ds.readUTF();
		String authType = ds.readUTF();

		if (authType.equals(ProtocolHPSP20.PINNO)) {

			// handle pin number authentication

			String pin = ds.readUTF();

			logger.log(Level.INFO, "Session created: " + sessionId);
			logger.log(Level.INFO, sessionId + ": AUTH_REQ message received from " + terminal + ": AUTH TYPE = " + authType);

			int role = RequestProcessor.getRoleForCard(pin, terminal);

			if (role == -1) {

				// role not found; possibly empty PIN number or card is not in the system

				this.terminalstate = TerminalState.DEMO;

				this.gamestate = Game.GAME_NONE;

				if (pin != null && pin.equals("")) {
					logger.log(Level.SEVERE, "Terminal authentication attempt using card unknown to a system; pin no : " + pin);
				}

				this.statecheck = TerminalState.DEMO;

				this.bank = 0;

				RequestProcessor.closeActiveTerminalSession(terminal, servicecard);

				sendCardNotFoundResponse(terminal, pin, dos);

			} else if (role == -2) {

				logger.log(Level.SEVERE, "!!!>>> Attempt to authenticate player card on blocked terminal : " + terminal);

			} else if (role == 1) {

				lastpin = pin;
				SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "lastpin", this.lastpin);

				AuthenticationResult auth = RequestProcessor.processAuthenticationPINRequest(terminal, pin);

				if (auth.isAuthenticated()) {

					if (this.gamestate != Game.GAME_NONE) {
						logger.log(Level.SEVERE, "Authentication attempt not in GAME_NONE state; game state = " + this.gamestate);
					}

					this.gamestate = Game.GAME_LOBBY;

					this.denomination = auth.getDenomination().getDenomination();

					this.cardTracker = new FurorCardTracker();

					cardTracker.checkSpecialCardStatus(auth);
					cardTracker.setDenomination(this.denomination);

					this.terminal = terminal;
					SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "terminal", this.terminal);

					this.cardno = pin;
					SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "cardno", this.cardno);

					this.lastpin = pin;
					SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "lastpin", this.lastpin);

					RequestProcessor.addSessionLogRecord(terminal, pin, sessionId);

					lastpins.put(this.terminal, this.lastpin);

					logger.log(Level.INFO, sessionId + ": PIN account balance moved to terminal balance:" + "\n" +
							               sessionId + ":    PIN  accnt bal = 0" + "\n" +
							               sessionId + ":    TERM accnt bal = " + auth.getAmount());

					int bank = auth.getAmount();

					this.bank = bank;

					if (bank < 0) {
						logger.log(Level.SEVERE, sessionId + " : BANK balance < 0 : BANK = " + bank);
					}

					sendAuthenticationResponse(terminal, bank, dos);

					setState(ConnectionSession.State.LOBBY_STATE);

					GameAvailability[] games = GameRegistry.getInstance().getGameAvailability(terminal);

					terminalstate = TerminalState.INPLAY;

					this.statecheck = TerminalState.LOBBY;

					sendGameAvailability(terminal, games, dos);

				} else {
					logger.log(Level.SEVERE, "Card can not be authenticated: pin = " + pin);

					terminalstate = TerminalState.DEMO;

					sendCardNotFoundResponse(terminal, pin, dos);
				}
			} else {
				logger.log(Level.INFO, sessionId + ": found service card for PIN " + pin + "; processing role = " + role);

				this.gamestate = Game.GAME_TECH;

				RequestProcessor.processAuthenticationTechRequest(terminal, pin, role);

				terminalstate = TerminalState.SERVICE;
				servicecard = pin;

				this.statecheck = TerminalState.SERVICE;

				sendTechCardResponse(terminal, role, dos);
			}

		} else if (authType.equals(ProtocolHPSP20.CCARD)) {
			logger.log(Level.SEVERE, "Unsupported authentication type: " + authType);
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
		} else if (authType.equals(ProtocolHPSP20.SMART)) {
			logger.log(Level.SEVERE, "Unsupported authentication type: " + authType);
			/*

			// handle smart card authentication
			@SuppressWarnings("unused")
			String cardNumber = ds.readUTF();

			// send the response back to the client

			 */
			return;
		} else {
			logger.log(Level.SEVERE, "Unsupported authentication type: " + authType);
			// Unknown auth type; return error
			return;
		}

		return;
	}


	private void sendTechCardResponse(String terminal, int role, DataOutputStreamEx dos) throws IOException {

		logger.log(Level.INFO, terminal + " : sending TCRD_RES from server to terminal; role = " + role);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_TCRD_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeInt(role);							// role - int

		this.sendMessage(bos, dos, ddos);
	}


	private void sendCardNotFoundResponse(String terminal, String pin, DataOutputStreamEx dos) throws IOException {

		logger.log(Level.INFO, terminal + " : sending NFND_RES from server to terminal; pin = " + pin);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_NFND_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeUTF(pin);							// role - int

		this.sendMessage(bos, dos, ddos);
	}


	private void sendAuthenticationResponse(String terminal, int balance, DataOutputStream dos)
																					throws IOException {

		logger.log(Level.INFO, terminal + " : sending AUTH_RES from server to terminal; BANK = " + balance);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_AUTH_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		ddos.writeInt(balance);							// BANK - int

		// additions to handle BANK/CREDIT/WIN reqs
		ddos.writeInt(0);	// CREDIT
		ddos.writeInt(0);	// WIN

		this.sendMessage(bos, dos, ddos);
	}


	private void sendGameAvailability(String terminal, GameAvailability[] games, DataOutputStream dos)
		throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_LIST_LOT);			// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeInt(games.length/3);

		logger.log(Level.FINE, "Sending game availability information to the terminal");

		String lastGame = "";

		for (int i = 0; i < games.length; i ++) {
			if (!lastGame.equals(games[i].getGame())) {
				lastGame = games[i].getGame();
				ddos.writeUTF(games[i].getGame());
				ddos.writeInt(3);
			}
			ddos.writeInt(games[i].getPrice());
			ddos.writeBoolean(games[i].available());
		}

		this.sendMessage(bos, dos, ddos);
	}
	
	private boolean inScratchMode() {
		return this.scratchmode;
	}


	private void handleDepositRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {
		// get other parameters from the request

		String terminal = ds.readUTF();
		String game     = ds.readUTF();

		logger.log(Level.INFO, sessionId + ": CASH_REQ message received from " + terminal + ": GAME = " + game);

		// fix for the defect GRUKR-872
		if (!inScratchMode()) {
			completeBonusTicket();
		}

		String ticketno = "UNKNOWN";
		int seqno = 0;

		if (this.bonusticket != null) {
			ticketno = bonusticket.getTicketNo();
			seqno = bonusticket.getPointer() + 2;
		} else {
			logger.log(Level.SEVERE, "Trying to deposit escrow win into CREDITS; ticket object is NULL");
		}

		DepositRequestResult res = RequestProcessor.processDepositRequest(terminal, game, ticketno, seqno);

		if (!res.success()) {
			throw new IOException("Error depositing winnings from ESCROW");
		}

		logger.log(Level.INFO, sessionId + ": game winnings are moved from game escrow into terminal account:\n" +
		                       sessionId + ":    ESCROW bal = 0\n" +
		                       sessionId + ":    CREDITS bal = " + res.getCredits());

		this.credits = (int) res.getCredits();
		this.win = 0;

		this.statecheck = TerminalState.SEG_REGULAR_ESCROW;

		sendDepositResponse(terminal, res.getBank(), res.getCredits(), res.getWin(), game, dos);

		setState(ConnectionSession.State.GAME_STATE);
	}


	private void sendDepositResponse(String terminal, long bank, long credits, long win, String game, DataOutputStream dos)
																						throws IOException {

		logger.log(Level.INFO, terminal + " : sending CASH_RES from server to terminal; game = " + game + "; BANK = " + bank + "; CREDITS = " + credits + "; WIN = " + win);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_CASH_RES);		// message ID - UTF
		ddos.writeUTF(terminal);				// terminal id - UTF

		ddos.writeLong(bank);					// BANK balance
		ddos.writeLong(credits);				// CREDITS balance
		ddos.writeLong(win);					// WIN balance

		ddos.writeUTF(game);					// game id - UTF

		this.sendMessage(bos, dos, ddos);
	}


	/**
	 * Handle deposit request sent by lottery terminal when new banknote is deposited into bill acceptor.
	 *
	 * @param ds
	 * @param dos
	 * @throws IOException
	 */
	private void handleDepositBNARequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();
		String game     = ds.readUTF();
		int amount      = ds.readInt();

		// Added 08.04.2009

		String lastpin = ds.readUTF();

		logger.log(Level.INFO, sessionId + ": DEPO_REQ message received from " + terminal + ": GAME = " + game + ": AMOUNT = " + amount);

		logger.log(Level.SEVERE, "Deposit BNA request: cardno =" + cardno + "; lastpin = " + lastpin);


		if (cardno == null && lastpin != null) {
			cardno = lastpin;
			SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "cardno", this.cardno);
		}

		// FIXME need to get the last used pin _on_this_terminal_ and use it if last PIN was zero

		try {
			DepositBNARequestResult res = RequestProcessor.processDepositBNARequest(terminal, game, amount, cardno);

			if (!res.success()) {
				throw new IOException("Unable to process BNA cash deposit");
			}

			logger.log(Level.INFO, sessionId + ": Player added money to terminal's BNA:\n" +
			                       sessionId + ":    amount of money added: " + amount + "\n" +
			                       sessionId + ":    BANK = " + res.getBank() + "; CREDITS = " + res.getCredits() + "; WIN = " + res.getWin());

			this.bank += amount;

			sendDepositBNAResponse(terminal, res.getBank(), res.getCredits(), res.getWin(), game, dos);

		} catch(WrongBanknoteException e) {
			logger.log(Level.SEVERE, "Wrong banknote exception caught", e);
			sendWrongBanknoteSubmitted(terminal, game, amount, dos);
		}
	}


	/**
	 * Send wrong banknote response to the terminal.
	 *
	 * Message structure:
	 * - message length (32-bit integer)
	 * - "DEPO_ERR" message type (UTF-8 string)
	 * - terminal id (UTF-8 string)
	 * - game id (UTF_8 string)
	 * - denomination sent to the server (32-bit integer)
	 *
	 * @param terminal		terminal identifier
	 * @param game			game identifier
	 * @param amount		denomination sent to the server
	 * @param dos			data output stream
	 */
	private void sendWrongBanknoteSubmitted(String terminal, String game, int amount, DataOutputStream dos)
																							throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_DEPO_ERR);		// message ID - UTF
		ddos.writeUTF(terminal);				// terminal id - UTF
		ddos.writeUTF(game);					// game id - UTF
		ddos.writeInt(amount);					// banknote denomination - int

		this.sendMessage(bos, dos, ddos);
	}


	private void sendDepositBNAResponse(String terminal, long bank, long credits, long win, String game, DataOutputStream dos)
																							throws IOException {

		logger.log(Level.INFO, terminal + " : sending DEPO_RES from server to terminal; game = " + game + "; BANK = " + bank + "; CREDITS = " + credits + "; WIN = " + win);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_DEPO_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		ddos.writeLong(bank);
		ddos.writeLong(credits);
		ddos.writeLong(win);

		ddos.writeUTF(game);								// game id - UTF

		this.sendMessage(bos, dos, ddos);
	}


	/**
	 * Handle cashout request from lottery terminal
	 *
	 * Moves funds from terminal account back to player card account, removes session from session manager,
	 * sends back cashout response, and set connection state to ConnectionSession.State.CONNECTED_STATE.
	 */
	private void handleCashOutRequest(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		String terminal = ds.readUTF();

		logger.log(Level.INFO, sessionId + ": COUT_REQ message received from " + terminal);

		if (cardno == null) {
			cardno = lastpins.get(terminal);
		}

		switch(getState().num()) {
			case 5:
			case 4:
			{
				logger.log(Level.SEVERE, sessionId + ": COUT_REQ message received from terminal " + terminal + " while terminal in ESCROW state; transitioning from ESCROW to PLAY state");
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: complete free games and bonuses: terminal = {0}, game = {1}", new Object[]{terminal, game});
				completeBonusTicket();
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: deposit WIN into CREDITS: terminal = {0}, game = {1}", new Object[]{terminal, game});
				RequestProcessor.processDepositRequest(terminal, game, this.bonusticket.getTicketNo(), (this.bonusticket.getPointer() + 2));
			}
			case 3:
			{
				logger.log(Level.SEVERE, sessionId + ": COUT_REQ message received from terminal " + terminal + " while terminal is in PLAY state; transitioning from PLAY to LOBBY state");
				String ticketno = bonusticket.getTicketNo();
				logger.log(Level.INFO, sessionId + " : SESSION CLEANUP: deposit CREDITS into BANK; terminal = {0}, game = {1}, ticket={2}", new Object[]{terminal, game, ticketno});
				RequestProcessor.processExitLotteryRequest(terminal, game, cardno, ticketno, utilized);
				game = null;
			}
		}

		int cardbalance = RequestProcessor.processCashoutRequest(terminal, cardno);

		logger.log(Level.INFO, sessionId + ": terminal account balance is transferred to pin account:\n" +
		                       sessionId + ":    terminal account balance = 0\n" +
		                       sessionId + ":    pin account balance = " + cardbalance);

		this.bank = 0;

		cardno = null;
		SessionManager.getSessionManager().setParamNoClusterUpdate(sessionId, "cardno", null);

		this.statecheck = TerminalState.DEMO;

		sendCashoutResponse(terminal, dos);

		setState(ConnectionSession.State.CONNECTED_STATE);
	}


	private void sendCashoutResponse(String terminal, DataOutputStream dos) throws IOException {

		logger.log(Level.INFO, terminal + " : sending COUT_RES from server to terminal; BANK = 0; CREDIT = 0; WIN = 0");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_COUT_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		// added to support BANK/CREDIT/WIN requirement
		ddos.writeInt(0);	// BANK
		ddos.writeInt(0);	// CREDIT
		ddos.writeInt(0);	// WIN

		this.sendMessage(bos, dos, ddos);
	}


	private void handleUnknownMessage(DataInputStreamEx ds, DataOutputStream dos) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_UMSG_RES);		// message ID - Unknown Message - UTF

		this.sendMessage(bos, dos, ddos);
	}
}

