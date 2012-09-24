package com.ninelinelabs.message.response;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class CardNotFoundResponse extends Response {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_NFND_RES;
	
	private final String terminal;
	private final String pin;
	
	public CardNotFoundResponse(String terminal, String pin) {
		this.terminal = terminal;
		this.pin = pin;
	}

	@Override
	public byte[] toByteArray() throws IOException {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(MSG_TYPE);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF
		ddos.writeUTF(pin);							// role - int
		
		return bos.toByteArray();
	}

	@Override
	public String toString() {
		
		return MSG_TYPE + " : { terminal : \"" + terminal + "\", pin : \"" + pin + "\" }";
	}
}
