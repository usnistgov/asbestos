package gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.xdm;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class XdmRequestResource implements XdmRequest {
    byte[] zip;

    public byte[] getZip() {
        return zip;
    }

    public void setZip(byte[] zip) {
        this.zip = zip;
    }
}
