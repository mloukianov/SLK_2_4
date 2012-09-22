package com.ninelinelabs.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class ParameterPutMessage extends Message {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_PARM_PUT;
	
	private String terminal;
	private int params;
	private String[] names;
	private String[] values;


	public ParameterPutMessage(byte[] msg) {
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
			params = dds.readInt();

			names = new String[params];
			values = new String[params];

			for (int i = 0; i < params; i++) {
				names[i] = dds.readUTF();
				values[i] = dds.readUTF();
			}
			
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
	
	public int getCount() {
		return this.params;
	}
	
	public String [] getNames() {
		return this.names;
	}
	
	public String[] getValues() {
		return this.values;
	}
}
