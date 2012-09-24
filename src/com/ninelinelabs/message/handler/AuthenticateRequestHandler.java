package com.ninelinelabs.message.handler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import com.ninelinelabs.authentication.vo.AuthenticationResult;
import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.io.DataOutputStreamEx;
import com.ninelinelabs.message.AuthenticateRequestMessage;
import com.ninelinelabs.message.Message;
import com.ninelinelabs.message.response.Response;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;
import com.ninelinelabs.server.ConnectionSession;
import com.ninelinelabs.server.Game;
import com.ninelinelabs.server.GameAvailability;
import com.ninelinelabs.server.GameRegistry;
import com.ninelinelabs.server.RequestProcessor;
import com.ninelinelabs.server.SessionManager;
import com.ninelinelabs.server.furor.FurorCardTracker;
import com.ninelinelabs.server.state.TerminalState;

public class AuthenticateRequestHandler extends MessageHandler {

	@Override
	public Response handle(Message message) throws SQLException {
		
		// cast to appopriate message type
		AuthenticateRequestMessage msg = (AuthenticateRequestMessage)message;
		
		// find the card object
		Card card = Card.findByPin(msg.getPin());
		
		// get terminal object based on the ID form ctx
		Terminal terminal = Terminal.findByName(ctx.getTerminalName());

		if (card == null) {
			// card not found
		} else if (card.getRole() == -1) {
			
		} else if (card.getRole() == -2) {
			
		} else if (card.getRole() == 1) {
			
		}
		
		return null;
	}

}

/*
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

		} */ 
