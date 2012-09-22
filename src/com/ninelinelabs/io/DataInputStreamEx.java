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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension class for java.io.DataInputStream
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 85 $ $Date: 2011-05-28 02:01:44 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class DataInputStreamEx extends DataInputStream {

	/**
	 * @param in
	 */
	public DataInputStreamEx(InputStream in) {
		super(in);
	}


	/**
	 * One-liner for reading array of 32-bit integers from java.io.DataInputStream
	 *
	 * @param size   size of the array to expect
	 *
	 * @return  array containing 32-bit integers
	 *
	 * @throws IOException
	 */
	public int[] readIntArray(int size) throws IOException {
		int[] array = new int[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = this.readInt();
		}
		return array;
	}
}
