package com.ninelinelabs.message;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;


public abstract class Message {
	
	public static final Logger logger = Logger.getLogger(Message.class.getName());
	
	private String msgtype = "";
	private byte[] msg;
	
	
	public Message(byte[] msg) {
		this.msg = msg;
	}
	
	
	protected byte[] getMsg() {
		return this.msg;
	}
	
	
	public int getMsglen() {
		return msg.length;
	}
	
	
	public String getMsgtype() {
		return this.msgtype;
	}
	
	
	protected void setMsgtype(String msgtype) {
		this.msgtype = msgtype;
	}

	
	public abstract Message parse() throws IOException;
}
