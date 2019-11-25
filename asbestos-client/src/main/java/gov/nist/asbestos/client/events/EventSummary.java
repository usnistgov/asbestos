package gov.nist.asbestos.client.events;

import gov.nist.asbestos.http.headers.Headers;

import java.io.File;

public class EventSummary {
    public String eventName;
    public String resourceType;
    String verb;
    boolean status;
    String ipAddr;

    public EventSummary(File eventFile) {
        try {
            UITask uiTask = new UITask(eventFile, "task0");
            String responseHeader = uiTask.getResponseHeader();
            Headers headers = new Headers(responseHeader);
            status = headers.getStatus() < 202;
            String requestHeader = uiTask.getRequestHeader();
            headers = new Headers(requestHeader);
            verb = headers.getVerb();
            ipAddr = headers.getHeaderValue("x-client-addr");
        } catch (Exception e) {
            status = false;
        }
    }
}
