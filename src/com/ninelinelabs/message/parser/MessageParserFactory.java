package com.ninelinelabs.message.parser;

public class MessageParserFactory {
	
	public MessageParser getParser(String format) {
		
		if (format == null) return null;
		
		if (format.equals("HPSP/2.0")) return new HPSP20MessageParser();
		
		return null;
	}
}
