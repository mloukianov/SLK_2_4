package com.ninelinelabs.message.response;

import java.io.IOException;

public abstract class Response {
	
	public abstract byte[] toByteArray() throws IOException;
	
	public abstract String toString();

}
