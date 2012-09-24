package com.ninelinelabs.message.response;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ninelinelabs.protocol.hpsp.ProtocolHPSP20;

public class AuthenticateResponse extends Response {
	
	public static final String MSG_TYPE = ProtocolHPSP20.HPSP_20_AUTH_RES;
	
	private final String terminal;
	private final int balance;

	public AuthenticateResponse(String terminal, int balance) {
		this.terminal = terminal;
		this.balance = balance;
	}
	
	@Override
	public byte[] toByteArray() throws IOException {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream ddos = new DataOutputStream(bos);

		ddos.writeUTF(ProtocolHPSP20.HPSP_20_AUTH_RES);		// message ID - UTF
		ddos.writeUTF(terminal);						// terminal id - UTF

		ddos.writeInt(balance);							// BANK - int

		// additions to handle BANK/CREDIT/WIN reqs
		ddos.writeInt(0);	// CREDIT
		ddos.writeInt(0);	// WIN
		
		return bos.toByteArray();
	}

	@Override
	public String toString() {
		
		return MSG_TYPE + " : { terminal : \"" + terminal + "\", bank : " + balance + ", credit : 0, win : 0 }";
	}
}
