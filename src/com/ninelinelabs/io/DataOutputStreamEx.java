/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: AuthenticationResult.java 85 2011-05-28 07:01:44Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension class for java.io.DataOutputStream
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 85 $ $Date: 2011-05-28 02:01:44 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class DataOutputStreamEx extends DataOutputStream {

	public DataOutputStreamEx(OutputStream os) {
		super(os);
	}

	/**
	 * One-liner for writing array of 32-bit integers into a stream
	 *
	 * @param array
	 * @throws IOException
	 */
	public void writeIntArray(int[] array) throws IOException {
		for (int a : array) {
			this.writeInt(a);
		}
	}
}
