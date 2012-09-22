package com.ninelinelabs.message.parser;

import java.io.IOException;

import com.ninelinelabs.message.Message;

public interface MessageParser {
	
	public Message parse(byte[] msg) throws IOException;

}
