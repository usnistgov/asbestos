package gov.nist.asbestos.client.events;


import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;

public class Task {
    private Event event;
    private int taskIndex;

    private Headers _requestHeaders = null;
    private byte[] _requestRawBody = null;
    private String _requestBody = null;

    private Headers _responseHeaders = null;
    private byte[] _responseRawBody = null;
    private String _responseBody = null;
    private String _description = null;


    Task(int taskIndex, Event event) {
        this.taskIndex = taskIndex;  // will be overwritten by initTask()
        // this allows tasks to be allocated but not used - initialization happens on first use
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    public Task newTask() {
        return event.newTask();
    }

    private void initTask() {
        if (taskIndex == Event.NEWTASK)
            taskIndex = event.initTask(this);
    }

    public boolean hasRun() {
        if (taskIndex == Event.NEWTASK)
            return false;
        if (_responseHeaders == null)
            return false;
        if (_responseHeaders.getStatus() != 0)
            return true;
        return false;
    }

    public void fromTask(HttpBase base) {
        base.setRequestHeaders(getRequestHeader());
        base.setRequest(getRequestBody());
        base.setRequestText(getRequestBodyAsString());
        base.setResponseHeaders(getResponseHeader());
        base.setStatus(_responseHeaders.getStatus());
        base.setResponse(getResponseBody());
        base.setRequestText(getResponseBodyAsString());
    }

    public void putRequestHeader(Headers headers) {
        _requestHeaders = headers;
        initTask();
        try {
            try (PrintWriter out = new PrintWriter(event.getRequestHeaderFile(taskIndex))) {
                out.print(headers.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestBody(byte[] body) {
        _requestRawBody = body;
        initTask();
        if (body != null) {
            _requestBody = new String(body);
            if (body.length > 0) {
                try {
                    try (FileOutputStream stream = new FileOutputStream(event.getRequestBodyFile(taskIndex))) {
                        stream.write(body);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public byte[] getRequestBody() {
        if (_requestRawBody == null) {
            if (_requestBody != null) {
                _requestRawBody = _requestBody.getBytes();
                return _requestRawBody;
            }
            try {
                _requestRawBody = Files.readAllBytes(event.getRequestBodyFile(taskIndex).toPath());
            } catch (Exception e) {
            }
            if (_requestRawBody != null)
                _requestBody = new String(_requestRawBody);
        }
        return _requestRawBody;
    }

    public Headers getRequestHeader() {
        if (_requestHeaders == null) {
            String headerString;
            try {
                headerString = new String(Files.readAllBytes(event.getRequestHeaderFile(taskIndex).toPath()));
            } catch (Exception e) {
                return new Headers();
            }
            _requestHeaders = new Headers(headerString);
        }
        return _requestHeaders;
    }

    public String getRequestBodyAsString() {
        getRequestBody();
        return _requestBody;
    }

    public void putResponseHeader(Headers headers) {
        _responseHeaders = headers;
        initTask();
        if (headers != null) {
            try {
                try (PrintWriter out = new PrintWriter(event.getResponseHeaderFile(taskIndex))) {
                    out.print(headers.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void putResponseBody(byte[] body) {
        _responseRawBody = body;
        initTask();
        if (body != null && body.length >  0) {
            try {
                try (FileOutputStream out = new FileOutputStream(event.getResponseBodyFile(taskIndex))) {
                    out.write(body);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void putResponseBodyText(String body) {
        _responseBody = body;
        initTask();
        try {
            try (PrintWriter out = new PrintWriter(event.getResponseBodyStringFile(taskIndex))) {
                out.print(body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putDescription(String description) {
        _description = description;
        initTask();
        try {
            try (PrintWriter out = new PrintWriter(event.getDescriptionFile(taskIndex))) {
                out.print(description);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestBodyText(String body) {
        initTask();
        try {
            try (PrintWriter out = new PrintWriter(event.getRequestBodyStringFile(taskIndex))) {
                out.print(body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putResponseHTMLBody(byte[] body) {
        initTask();
        putResponseBody(body);
        String bodyString = new String(body);
        putResponseBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(event.getResponseBodyHTMLFile(taskIndex))) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestHTMLBody(byte[] body) {
        initTask();
        putRequestBody(body);
        String bodyString = new String(body);
        putRequestBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(event.getRequestBodyHTMLFile(taskIndex))) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Headers getResponseHeader() {
        if (_responseHeaders == null) {
            try {
                String resp = new String(Files.readAllBytes(event.getResponseHeaderFile(taskIndex).toPath()));
                _responseHeaders = new Headers(resp);
            } catch (Exception e) {

            }
        }
        return _responseHeaders;
    }

    public byte[] getResponseBody() {
        if (_responseRawBody == null) {
            if (_responseBody != null) {
                _responseRawBody = _responseBody.getBytes();
                return _responseRawBody;
            }
            try {
                _responseRawBody = Files.readAllBytes(event.getResponseBodyFile(taskIndex).toPath());
                _responseBody = new String(_responseRawBody);
            } catch (Exception e) {

            }
        }
        return _responseRawBody;
    }

    public String getDescription() {
        if (_description == null) {
            try {
                _description = new String(Files.readAllBytes(event.getResponseBodyFile(taskIndex).toPath()));
            } catch (Exception e) {

            }
        }
        return _description;
    }

    public String getResponseBodyAsString() {
        getResponseBody();
        return _responseBody;
    }
}
