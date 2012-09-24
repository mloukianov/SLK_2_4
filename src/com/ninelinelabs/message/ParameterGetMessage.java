package com.ninelinelabs.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class ParameterGetMessage extends Message {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_PARM_GET;
	
	private String terminal;
	private String param;


	public ParameterGetMessage(byte[] msg) {
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
			
			terminal = dds.readUTF();
			param = dds.readUTF();
			
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
	
	public String getName() {
		return this.param;
	}
}
