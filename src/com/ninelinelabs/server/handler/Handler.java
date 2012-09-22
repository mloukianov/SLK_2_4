/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Handler.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Sep 16, 2010 mloukianov Created
 *
 */
package com.ninelinelabs.server.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException;

import com.ninelinelabs.server.session.TerminalSessionContext;

/**
 * Message handler interface
 * 
 * @author mloukianov
 */
public interface Handler {
	
	public String getMessageType();
	
	public void service(DataInputStream dis, DataOutputStream dos, TerminalSessionContext ctx) throws IOException;
}
