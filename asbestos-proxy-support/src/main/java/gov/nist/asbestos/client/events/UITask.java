package gov.nist.asbestos.client.events;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;

import java.io.File;
import java.util.Set;

public class UITask {
    private int index;
    private String label;
    private String description;
    private String requestHeader;
    private String requestBody;
    private String responseHeader;
    private String responseBody;

    public UITask(File eventDir, String taskLabel, Set<TaskPartEnum> e) {
        e.forEach(s -> {
            if (s.equals(TaskPartEnum.REQUEST_HEADER)) {
                requestHeader = readFile(eventDir, taskLabel, "request_header.txt", true );
            } else if (s.equals(TaskPartEnum.RESPONSE_HEADER)) {
                responseHeader = readFile(eventDir, taskLabel, "response_header.txt", true );
            }
        });
    }

    public UITask(File eventDir, String taskLabel, boolean rawTextMode) {
        description = readFile(eventDir, taskLabel,"description.txt" , true);
        requestHeader = readFile(eventDir, taskLabel, "request_header.txt" ,true );
        responseHeader = readFile(eventDir, taskLabel, "response_header.txt", true );

       if (rawTextMode) {
           requestBody = readFile(eventDir, taskLabel, "request_body.bin", false);
           responseBody = readFile(eventDir, taskLabel, "response_body.bin", false);
       } else {
           requestBody = readFile(eventDir, taskLabel, "request_body.txt", true);
           responseBody = readFile(eventDir, taskLabel, "response_body.txt", true);
       }
    }

    private String readFile(File eventDir, String taskLabel, String fileName, boolean fallBackToTxt) {
        try {
            return Reader.read(eventDir, taskLabel, fileName, fallBackToTxt);
        } catch (Exception ex) {
            return  "UITask Exception: " + fileName + " could not be read. " + ex.toString();
        }
    }

    public UITask(File eventDir, String taskLabel) {
        this(eventDir, taskLabel, false);
    }

    public UITask(ResourceWrapper wrapper) {
        index = 0;
        label = "";
        description = wrapper.getRef().toString();
        requestHeader = new Headers().withContentType(Format.JSON.getContentType()).toString();
        requestBody = ParserBase.encode(wrapper.getResource(), Format.JSON);
        responseBody = "";
        responseHeader = "";
    }

    public boolean isHTMLResponse() {
        Headers headers = new Headers(responseHeader);
        return headers.getContentType().getValue().contains("html");
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getRequestHeader() {
        return requestHeader;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseHeader() {
        return responseHeader;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
