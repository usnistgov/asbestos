package gov.nist.asbestos.asbestosProxy.event;

import gov.nist.asbestos.client.events.Reader;
import gov.nist.asbestos.http.headers.Headers;

import java.io.File;

public class EventSummary {
    String eventName;
    String resourceType;
    String verb;
    boolean status;

    public EventSummary(File eventFile) {
        try {
            String responseHeader = Reader.read(eventFile, "task0", "response_header.txt");
            Headers headers = new Headers(responseHeader);
            status = headers.getStatus() < 202;
            String requestHeader = Reader.read(eventFile, "task0", "request_header.txt");
            headers = new Headers(requestHeader);
            verb = headers.getVerb();
        } catch (Exception e) {
            status = false;
        }
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
