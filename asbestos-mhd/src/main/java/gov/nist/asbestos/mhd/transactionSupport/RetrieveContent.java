package gov.nist.asbestos.mhd.transactionSupport;

public class RetrieveContent {
    private String contentType;
    private byte[] content;

    public RetrieveContent(String contentType, byte[] content) {
        this.contentType = contentType;
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
