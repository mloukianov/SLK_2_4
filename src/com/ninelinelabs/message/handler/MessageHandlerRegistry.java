package com.ninelinelabs.message.handler;

import java.util.HashMap;

import com.ninelinelabs.message.AuthenticateRequestMessage;
import com.ninelinelabs.message.BonusRequestMessage;
import com.ninelinelabs.message.BuyTicketRequestMessage;
import com.ninelinelabs.message.CashoutRequestMessage;
import com.ninelinelabs.message.ConnectRequestMessage;
import com.ninelinelabs.message.CountRequestMessage;
import com.ninelinelabs.message.DealerCardRequestMessage;
import com.ninelinelabs.message.DepositBnaRequestMessage;
import com.ninelinelabs.message.DepositWinRequestMessage;
import com.ninelinelabs.message.DoubleRequestMessage;
import com.ninelinelabs.message.EndCountRequestMessage;
import com.ninelinelabs.message.ExitTicketRequestMessage;
import com.ninelinelabs.message.LogEntryRequestMessage;
import com.ninelinelabs.message.ParameterGetMessage;
import com.ninelinelabs.message.ParameterPutMessage;
import com.ninelinelabs.message.PingRequestMessage;
import com.ninelinelabs.message.PlayRequestMessage;
import com.ninelinelabs.message.PrintConfirmMessage;
import com.ninelinelabs.message.PrintRequestMessage;
import com.ninelinelabs.message.ReconnectRequestMessage;
import com.ninelinelabs.message.ReelRequestMessage;
import com.ninelinelabs.message.RollEndMessage;
import com.ninelinelabs.message.RollErrorMessage;
import com.ninelinelabs.message.RollInitMessage;
import com.ninelinelabs.message.RollTicketRequestMessage;

public class MessageHandlerRegistry {
	
	private HashMap<String, MessageHandler> handlers = new HashMap<String, MessageHandler>();

	
	public MessageHandlerRegistry() {
		
		populateRegistry();
	}
	
	
	private void populateRegistry() {
		
		handlers.put(AuthenticateRequestMessage.MSG_TYPE, new AuthenticateRequestHandler());
		handlers.put(BonusRequestMessage.MSG_TYPE, new BonusRequestHandler());
		handlers.put(BuyTicketRequestMessage.MSG_TYPE, new BuyTicketRequestHandler());
		handlers.put(CashoutRequestMessage.MSG_TYPE, new CashoutRequestHandler());
		handlers.put(ConnectRequestMessage.MSG_TYPE, new ConnectRequestHandler());
		handlers.put(CountRequestMessage.MSG_TYPE, new CountRequestHandler());
		handlers.put(DealerCardRequestMessage.MSG_TYPE, new DealerCardRequestHandler());
		handlers.put(DepositBnaRequestMessage.MSG_TYPE, new DepositBnaRequestHandler());
		handlers.put(DepositWinRequestMessage.MSG_TYPE, new DepositWinRequestHandler());
		handlers.put(DoubleRequestMessage.MSG_TYPE, new DoubleRequestHandler());
		handlers.put(EndCountRequestMessage.MSG_TYPE, new EndCountRequestHandler());
		handlers.put(ExitTicketRequestMessage.MSG_TYPE, new ExitTicketRequestHandler());
		handlers.put(LogEntryRequestMessage.MSG_TYPE, new LogEntryRequestHandler());
		handlers.put(ParameterGetMessage.MSG_TYPE, new ParameterGetHandler());
		handlers.put(ParameterPutMessage.MSG_TYPE, new ParameterPutHandler());
		handlers.put(PingRequestMessage.MSG_TYPE, new PingRequestHandler());
		handlers.put(PlayRequestMessage.MSG_TYPE, new PlayRequestHandler());
		handlers.put(PrintConfirmMessage.MSG_TYPE, new PrintConfirmHandler());
		handlers.put(PrintRequestMessage.MSG_TYPE, new PrintRequestHandler());
		handlers.put(ReconnectRequestMessage.MSG_TYPE, new ReconnectRequestHandler());
		handlers.put(ReelRequestMessage.MSG_TYPE, new ReelRequestHandler());
		handlers.put(RollEndMessage.MSG_TYPE, new RollEndHandler());
		handlers.put(RollErrorMessage.MSG_TYPE, new RollErrorHandler());
		handlers.put(RollInitMessage.MSG_TYPE, new RollInitHandler());
		handlers.put(RollTicketRequestMessage.MSG_TYPE, new RollTicketRequestHandler());
	}

	
	public synchronized void registerHandler(String message, MessageHandler handler) {
		handlers.put(message, handler);
	}
	
	
	public MessageHandler getHandler(String message) {
		return handlers.get(message);
	}
}
