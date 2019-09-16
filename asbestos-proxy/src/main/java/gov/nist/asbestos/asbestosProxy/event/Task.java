package gov.nist.asbestos.asbestosProxy.event;

import java.io.File;

public class Task {
    private int index;
    private String label;
    private String description;
    private String requestHeader;
    private String requestBody;
    private String responseHeader;
    private String responseBody;

    public Task(File eventDir, String taskLabel) {
        description = Reader.read(eventDir, taskLabel, "description.txt");
        requestHeader = Reader.read(eventDir, taskLabel, "request_header.txt");
        requestBody = Reader.read(eventDir, taskLabel, "request_body.txt");
        responseHeader = Reader.read(eventDir, taskLabel, "response_header.txt");
        responseBody = Reader.read(eventDir, taskLabel, "response_body.txt");
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
