package gov.nist.asbestos.client.events;

import gov.nist.asbestos.http.headers.Headers;

import java.io.File;
import java.util.Collections;

public class EventSummary {
    public String eventName;
    public String resourceType;
    String verb;
    Boolean status;
    String ipAddr;

    public EventSummary() {
    }

    public EventSummary(File eventFile) {
        try {
            UITask uiTask = new UITask(eventFile, "task0");
            String responseHeader = uiTask.getResponseHeader();
            Headers headers = new Headers(responseHeader);
            status = new Boolean (headers.getStatus() < 202);
            loadHeader(uiTask);
        } catch (Exception e) {
            status = false;
        }
    }

    public void loadRequestUiTask(File eventFile) {
        UITask uiTask = new UITask(eventFile, "task0", Collections.singleton(TaskPartEnum.REQUEST_HEADER));
        loadHeader(uiTask);
    }

    private void loadHeader(UITask uiTask) {
        String requestHeader = uiTask.getRequestHeader();
        if (requestHeader != null) {
            Headers headers = new Headers(requestHeader);
            verb = headers.getVerb();
            ipAddr = headers.getHeaderValue("x-client-addr");
        } else {
            verb = "NotAvail";
            ipAddr = "NotAvail";
        }
    }
}
