package com.ninelinelabs.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class ConnectRequestMessage extends Message {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_CONN_REQ;

	private String card;
	private String ticket;

	public ConnectRequestMessage(byte[] msg) {
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
			
			card = dds.readUTF();
			ticket = dds.readUTF();
			
		} catch (IOException e) {
			
			logger.log(Level.SEVERE, "can not parse message " + MSG_TYPE, e);
			throw e;
			
		} finally {
			try { dds.close(); } catch(Exception e) {}
		}
		
		return this;
	}

	public String getCard() {
		return this.card;
	}
	
	public String getTicket() {
		return this.ticket;
	}
}
