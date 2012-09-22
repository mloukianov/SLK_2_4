/*
 * Copyright (C) 2008-2009, Nine Line Labs, Inc.
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs Inc. or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SessionUpdate.java 95 2011-05-28 07:14:06Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

import java.io.Serializable;

public class SessionUpdate implements Serializable {

	private static final long serialVersionUID = 4934589286816933877L;

	public String session;
	public String key;
	public Serializable value;
	public boolean remove;
}
