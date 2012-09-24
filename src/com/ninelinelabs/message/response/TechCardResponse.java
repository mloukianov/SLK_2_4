package com.ninelinelabs.message.response;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class TechCardResponse extends Response {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_TCRD_RES;
	
	private final String terminal;
	private final int role;
	
	public TechCardResponse(String terminal, int role) {
		this.terminal = terminal;
		this.role = role;
	}

	@Override
	public byte[] toByteArray() throws IOException {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(MSG_TYPE);		// message ID - UTF
		ddos.writeUTF(terminal);		// terminal id - UTF
		ddos.writeInt(role);			// role - int
		
		return bos.toByteArray();
	}

	@Override
	public String toString() {
		
		return MSG_TYPE + " : { terminal : \"" + terminal + "\", role : \"" + role + "\" }";
	}

}
