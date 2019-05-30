package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;

/**
 *
 */
public class Attachment {
    private String contentType;
    private byte[] content;
    private String contentId;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
}

