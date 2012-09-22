package com.ninelinelabs.util.webservice;

import javax.xml.ws.BindingProvider;

public class WebServiceUtils {

	public static void setEndpointUrl(BindingProvider bp, String url) {
		bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
	}
}
