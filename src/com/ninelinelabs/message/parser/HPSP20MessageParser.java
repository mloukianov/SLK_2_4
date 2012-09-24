package com.ninelinelabs.message.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ninelinelabs.io.DataInputStreamEx;

import com.ninelinelabs.message.*;

public class HPSP20MessageParser implements MessageParser {
	
	public static final Logger logger = Logger.getLogger(HPSP20MessageParser.class.getName());

	@Override
	public Message parse(byte[] msg) throws IOException {
		
		DataInputStreamEx dds = null;
		
		try {
		
			dds = new DataInputStreamEx(new ByteArrayInputStream(msg));
			
			String type = dds.readUTF();
			
			if (AuthenticateRequestMessage.MSG_TYPE.equals(type)) {
				
				return new AuthenticateRequestMessage(msg).parse();
				
			} else if (BonusRequestMessage.MSG_TYPE.equals(type)) {
				
				return new BonusRequestMessage(msg).parse();
				
			} else if (BuyTicketRequestMessage.MSG_TYPE.equals(type)) {
				
				return new BuyTicketRequestMessage(msg).parse();
				
			} else if (CashoutRequestMessage.MSG_TYPE.equals(type)) {
				
				return new CashoutRequestMessage(msg).parse();
				
			} else if (ConnectRequestMessage.MSG_TYPE.equals(type)) {
				
				return new ConnectRequestMessage(msg).parse();
				
			} else if (CountRequestMessage.MSG_TYPE.equals(type)) {
				
				return new CountRequestMessage(msg).parse();
				
			} else if (DealerCardRequestMessage.MSG_TYPE.equals(type)) {
				
				return new DealerCardRequestMessage(msg).parse();
				
			} else if (DepositBnaRequestMessage.MSG_TYPE.equals(type)) {
				
				return new DepositBnaRequestMessage(msg).parse();
				
			} else if (DoubleRequestMessage.MSG_TYPE.equals(type)) {
				
				return new DoubleRequestMessage(msg).parse();
				
			} else if (EndCountRequestMessage.MSG_TYPE.equals(type)) {
				
				return new EndCountRequestMessage(msg).parse();
				
			} else if (ExitTicketRequestMessage.MSG_TYPE.equals(type)) {
				
				return new ExitTicketRequestMessage(msg).parse();
				
			} else if (LogEntryRequestMessage.MSG_TYPE.equals(type)) {
				
				return new LogEntryRequestMessage(msg).parse();
				
			} else if (ParameterGetMessage.MSG_TYPE.equals(type)) {
				
				return new ParameterGetMessage(msg).parse();
				
			} else if (ParameterPutMessage.MSG_TYPE.equals(type)) {
				
				return new ParameterPutMessage(msg).parse();
				
			} else if (PingRequestMessage.MSG_TYPE.equals(type)) {
				
				return new PingRequestMessage(msg).parse();
				
			} else if (PlayRequestMessage.MSG_TYPE.equals(type)) {
				
				return new PlayRequestMessage(msg).parse();
				
			} else if (PrintConfirmMessage.MSG_TYPE.equals(type)) {
				
				return new PrintConfirmMessage(msg).parse();
				
			} else if (PrintRequestMessage.MSG_TYPE.equals(type)) {
				
				return new PrintRequestMessage(msg).parse();
				
			} else if (ReconnectRequestMessage.MSG_TYPE.equals(type)) {
				
				return new ReconnectRequestMessage(msg).parse();
				
			} else if (ReelRequestMessage.MSG_TYPE.equals(type)) {
				
				return new ReelRequestMessage(msg).parse();
				
			} else if (RollEndMessage.MSG_TYPE.equals(type)) {
				
				return new RollEndMessage(msg).parse();
				
			} else if (RollErrorMessage.MSG_TYPE.equals(type)) {
				
				return new RollErrorMessage(msg).parse();
				
			} else if (RollInitMessage.MSG_TYPE.equals(type)) {
				
				return new RollInitMessage(msg).parse();
				
			} else if (RollTicketRequestMessage.MSG_TYPE.equals(type)) {
				
				return new RollTicketRequestMessage(msg).parse();
				
			} else {
				logger.log(Level.SEVERE, "Can not find message for type: " + type);
				
				return null;
			}
			
		} catch (IOException e) {
			
			logger.log(Level.SEVERE, "can not parse message", e);
			throw e;
			
		} finally {
			try { dds.close(); } catch(Exception e) {}
		}
	}

}
