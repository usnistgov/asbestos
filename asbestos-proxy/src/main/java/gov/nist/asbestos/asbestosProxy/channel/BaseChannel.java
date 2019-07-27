package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.client.client.Format;

public abstract class BaseChannel implements IBaseChannel {
    protected Format returnFormatType = null;

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }
}
