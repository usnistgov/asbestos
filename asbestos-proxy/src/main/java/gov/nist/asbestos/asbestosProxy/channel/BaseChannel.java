package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.asbestosProxy.events.Event;
import gov.nist.asbestos.client.client.Format;

public abstract class BaseChannel implements IBaseChannel {
    protected Format returnFormatType = null;
    private Event event = null;
    private String hostport = null;

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }

    public Format getReturnFormatType() {
        return returnFormatType;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getHostport() {
        return hostport;
    }

    public void setHostport(String hostport) {
        this.hostport = hostport;
    }
}
