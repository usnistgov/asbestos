package gov.nist.asbestos.asbestosProxy.wrapper;

import gov.nist.asbestos.client.client.Format;

public class TransformException extends RuntimeException {
    private String response = null;
    private Format format;

    public TransformException(String response, Format format) {
        this.response = response;
        this.format = format;
    }

    public String getResponse() {
        return response;
    }

    public Format getFormat() {
        return format;
    }
}
