package gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.DocumentContent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class DocumentContentResource implements DocumentContent {
    String uniqueId;
    byte[] content;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
