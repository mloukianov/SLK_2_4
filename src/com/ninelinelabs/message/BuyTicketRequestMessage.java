package com.ninelinelabs.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class BuyTicketRequestMessage extends Message {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_TIKT_REQ;
	
	private String terminal;
	private String game;
	private int price;
	private String paperticket;

	public BuyTicketRequestMessage(byte[] msg) {
		super(msg);
	}

	@Override
	public Message parse() throws IOException {
		
		DataInputStreamEx dds = null;
		
		try {
		
			dds = new DataInputStreamEx(new ByteArrayInputStream(getMsg()));
			
			String type = dds.readUTF();
			
			if (!MSG_TYPE.equals(type)) {
				assert(true);
			}
			
			this.setMsgtype(type);
			
			terminal = dds.readUTF();		// terminal id
			game = dds.readUTF();		// game id
			price = dds.readInt();		// ticket price
			paperticket = dds.readUTF();		// paper ticket number; UA-specific!
			
		} catch (IOException e) {
			
			logger.log(Level.SEVERE, "can not parse message " + MSG_TYPE, e);
			throw e;
			
		} finally {
			try { dds.close(); } catch(Exception e) {}
		}
		
		return this;
	}
	
	public String getTerminal() {
		return this.terminal;
	}
	
	public String getGame() {
		return this.game;
	}
	
	public int getPrice() {
		return this.price;
	}
	
	public String getTicket() {
		return this.paperticket;
	}
}
