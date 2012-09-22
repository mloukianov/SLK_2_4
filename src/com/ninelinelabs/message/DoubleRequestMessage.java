package com.ninelinelabs.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class DoubleRequestMessage extends Message {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_DUBL_REQ;
	
	private String terminal;
	private String game;
	private int seq;
	private int bet;
	private int color;

	public DoubleRequestMessage(byte[] msg) {
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
			
			terminal = dds.readUTF();
			game = dds.readUTF();
			seq = dds.readInt();
			bet = dds.readInt();
			color = dds.readInt();
			
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
	
	public int getSeq() {
		return this.seq;
	}
	
	public int getBet() {
		return this.bet;
	}
	
	public int getColor() {
		return this.color;
	}
}
