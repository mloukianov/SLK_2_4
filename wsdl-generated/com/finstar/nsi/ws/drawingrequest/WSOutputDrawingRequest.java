
package com.finstar.nsi.ws.drawingrequest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="requestResult" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "requestResult"
})
@XmlRootElement(name = "WSOutputDrawingRequest")
public class WSOutputDrawingRequest {

    protected boolean requestResult;

    /**
     * Gets the value of the requestResult property.
     * 
     */
    public boolean isRequestResult() {
        return requestResult;
    }

    /**
     * Sets the value of the requestResult property.
     * 
     */
    public void setRequestResult(boolean value) {
        this.requestResult = value;
    }

}
