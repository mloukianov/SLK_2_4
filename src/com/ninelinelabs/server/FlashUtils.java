/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: FlashUtils.java 137 2011-06-04 04:58:17Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * July 01, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server;

import java.io.IOException;

import com.ninelinelabs.io.DataInputStreamEx;
import com.ninelinelabs.io.DataOutputStreamEx;

/**
 * Utility class to send Adobe Flash policy file back
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 137 $ $Date: 2011-06-03 23:58:17 -0500 (Fri, 03 Jun 2011) $
 * @see
 */
public class FlashUtils {

	public static final int MAGIC_NUMBER = 1014001516;


	/**
	 * Sends policy file when Flash Player request is received
	 *
	 * @param ds  data input stream
	 * @param dos
	 * @throws IOException
	 */
	public static void sendPolicyFile(DataInputStreamEx ds, DataOutputStreamEx dos) throws IOException {

		char nextChar = ' ';

		while (nextChar != '\0') {
			nextChar = (char)ds.read();
		}

		String policyResponse = "";

		policyResponse += "<cross-domain-policy>";
		policyResponse += "<allow-access-from domain=\"*\" to-ports=\"*\"/>";
		policyResponse += "</cross-domain-policy>\0";

		byte[] b = policyResponse.getBytes();

		dos.write(b);
		dos.flush();
	}
}
