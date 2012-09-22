
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
 *         &lt;element name="clubId" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="specialCardId" type="{http://www.w3.org/2001/XMLSchema}long"/>
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
    "clubId",
    "specialCardId"
})
@XmlRootElement(name = "WSInputDrawingRequest")
public class WSInputDrawingRequest {

    protected long clubId;
    protected long specialCardId;

    /**
     * Gets the value of the clubId property.
     * 
     */
    public long getClubId() {
        return clubId;
    }

    /**
     * Sets the value of the clubId property.
     * 
     */
    public void setClubId(long value) {
        this.clubId = value;
    }

    /**
     * Gets the value of the specialCardId property.
     * 
     */
    public long getSpecialCardId() {
        return specialCardId;
    }

    /**
     * Sets the value of the specialCardId property.
     * 
     */
    public void setSpecialCardId(long value) {
        this.specialCardId = value;
    }

}
