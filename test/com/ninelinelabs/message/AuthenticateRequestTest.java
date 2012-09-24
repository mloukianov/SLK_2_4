/*
 * Copyright (C) 2008-2012, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: $
 *
 * Date Author Changes
 * Sep 22, 2012 maxloukianov Created
 *
 */
package com.ninelinelabs.message;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author maxloukianov
 *
 */
public class AuthenticateRequestTest {
	
	private byte[] msg;
	private byte[] shortmsg;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		DataOutputStream dos = null;
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			dos = new DataOutputStream(bos);
			
			dos.writeUTF(AuthenticateRequestMessage.MSG_TYPE);
			dos.writeUTF("T0001");
			dos.writeUTF("PIN");
			dos.writeUTF("1234567890");
			
			msg = bos.toByteArray();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { dos.close(); } catch(Exception e) {}
		}
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			dos = new DataOutputStream(bos);
			
			dos.writeUTF(AuthenticateRequestMessage.MSG_TYPE);
			dos.writeUTF("T0001");
			dos.writeUTF("PIN");
			// dos.writeUTF("1234567890");
			
			shortmsg = bos.toByteArray();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { dos.close(); } catch(Exception e) {}
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link com.ninelinelabs.message.AuthenticateRequestMessage#parse()}.
	 */
	@Test
	public final void testParse() throws Exception {
		
		AuthenticateRequestMessage message = new AuthenticateRequestMessage(msg);
		Message parsedMessage = message.parse();
	}

	/**
	 * Test method for {@link com.ninelinelabs.message.Message#getMsgtype()}.
	 */
	@Test(expected = EOFException.class)
	public final void testParseMessageThatIsTooShort() throws Exception {
		
		AuthenticateRequestMessage message = new AuthenticateRequestMessage(shortmsg);
		Message parsedMessage = message.parse();
	}

}
