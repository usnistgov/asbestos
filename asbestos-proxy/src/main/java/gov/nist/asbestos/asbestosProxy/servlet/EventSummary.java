package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.http.headers.Headers;

import java.io.File;

public class EventSummary {
    public String eventName;
    public String resourceType;
    String verb;
    boolean status;

    public EventSummary(File eventFile) {
        try {
            String responseHeader = Task.read(eventFile, "task0", "response_header.txt");
            Headers headers = new Headers(responseHeader);
            status = headers.getStatus() < 202;
            String requestHeader = Task.read(eventFile, "task0", "request_header.txt");
            headers = new Headers(requestHeader);
            verb = headers.getVerb();
        } catch (Exception e) {
            status = false;
        }
    }
}
