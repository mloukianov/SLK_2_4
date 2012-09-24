package com.ninelinelabs.message.handler;

import java.sql.SQLException;

import com.ninelinelabs.message.Message;
import com.ninelinelabs.message.response.Response;

public abstract class MessageHandler {
	
	protected HandlerContext ctx;
	
	public void setContext(HandlerContext ctx) {
		this.ctx = ctx;
	}
	
	public abstract Response handle(Message message) throws SQLException;
}
