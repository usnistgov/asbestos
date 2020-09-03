package gov.nist.asbestos.client.events;


import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.util.Gzip;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Objects;

public class Task implements ITask {
    private Event event;
    private int taskIndex;

    private Headers _requestHeaders = null;
    private byte[] _requestRawBody = null;
    private String _requestBody = null;  // this is plain text even if bytes are zipped

    private Headers _responseHeaders = null;
    private byte[] _responseRawBody = null;
    private String _responseBody = null;  // this is plain text even if bytes are zipped
    private String _description = null;

    Task(int taskIndex, Event event) {
        this.taskIndex = taskIndex;  // will be overwritten by initTask()
        // this allows tasks to be allocated but not used - initialization happens on first use
        this.event = event;
    }

    // Re-create HttpBase from Task
    @Override
    public HttpBase getHttpBase() {
        HttpBase base = null;
        String verb = getVerb();
        if ("GET".equalsIgnoreCase(verb))
            base = new HttpGetter();
        else if ("POST".equalsIgnoreCase(verb))
            base = new HttpPost();
        else if ("DELETE".equalsIgnoreCase(verb))
            base = new HttpDelete();
        else
            throw new Error("Cannot translate Task into HttpBase - Headers are " + _requestHeaders);
        fromTask(base);
        return base;
    }

    @Override
    public String getVerb() {
        Headers headers = getRequestHeader();
        return headers.getVerb();
    }

    @Override
    public Event getEvent() {
        return event;
    }

    @Override
    public ITask newTask() {
        return event.newTask();
    }

    private void initTask() {
        if (taskIndex == Event.NEWTASK)
            taskIndex = event.initTask(this);
    }

    @Override
    public boolean hasRun() {
        if (taskIndex == Event.NEWTASK)
            return false;
        if (_responseHeaders == null)
            return false;
        if (_responseHeaders.getStatus() != 0)
            return true;
        return false;
    }

    @Override
    public void fromTask(HttpBase base) {
        base.setRequestHeaders(getRequestHeader());
        base.setRequest(getRequestBody());
        base.setRequestText(getRequestBodyAsString());
        base.setResponseHeaders(getResponseHeader());
        base.setStatus(_responseHeaders.getStatus());
        base.setResponse(getResponseBody());
        base.setRequestText(getResponseBodyAsString());
    }

    @Override
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

    public static byte[] unzip(byte[] in) {
        try {
            byte[] un = Gzip.decompressGZIP(in);
            return un;
        } catch (Exception e) {
            return in;
        }
    }

    public static String unzip(String in) {
        try {
            String un = Gzip.decompressGZIPToString(in.getBytes());
            return un;
        } catch (Exception e) {
            return in;
        }
    }

    @Override
    public void putRequestBody(byte[] body) {
        Objects.requireNonNull(_requestHeaders);
        _requestRawBody = body;
        initTask();
        if (body != null) {
            _requestBody = new String(unzip(body));
//            if (_requestHeaders.isZipped())
//                _requestBody = Gzip.decompressGZIPToString(body);
//            else
//                _requestBody = new String(body);
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

    // returns unzipped content
    @Override
    public byte[] getRequestBody() {
        Objects.requireNonNull(_requestHeaders);
        if (_requestRawBody == null) {
            if (_requestBody != null) {
                _requestRawBody = _requestBody.getBytes();
                return _requestRawBody;
            }
            try {
                _requestRawBody = Files.readAllBytes(event.getRequestBodyFile(taskIndex).toPath());
            } catch (Exception e) {
            }
            if (_requestRawBody != null) {
//                if (_requestHeaders.isZipped())
//                    _requestBody = Gzip.decompressGZIPToString(_requestRawBody);
//                else
                    _requestBody = new String(_requestRawBody);
            }
        }
//        if (_requestHeaders.isZipped())
//            return Gzip.decompressGZIP(_requestRawBody);
        if (_requestBody == null && _requestRawBody != null)
            _requestBody = new String(_requestRawBody);
        return _requestRawBody;
    }

    @Override
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

    @Override
    public String getRequestBodyAsString() {
        getRequestBody();
        return _requestBody;
    }

    @Override
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

    @Override
    public void putResponseBody(byte[] body) {
        body = unzip(body);  // just in case
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void putResponseHTMLBody(byte[] body) {
        Objects.requireNonNull(_responseHeaders);
        initTask();
        putResponseBody(body);
        String bodyString;
//        if (_responseHeaders.isZipped())
//            bodyString = Gzip.decompressGZIPToString(body);
//        else
            bodyString = new String(body);
        putResponseBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(event.getResponseBodyHTMLFile(taskIndex))) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putRequestHTMLBody(byte[] body) {
        Objects.requireNonNull(_requestHeaders);
        initTask();
        putRequestBody(body);
        String bodyString;
//        if (_requestHeaders.isZipped())
//            bodyString = Gzip.decompressGZIPToString(body);
//        else
            bodyString = new String(body);
        putRequestBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(event.getRequestBodyHTMLFile(taskIndex))) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
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

    @Override
    public byte[] getResponseBody() {
        //Objects.requireNonNull(_responseHeaders);
        if (_responseRawBody == null) {
            if (_responseBody != null) {
//                if (_responseHeaders.isZipped())
//                    _responseRawBody = Gzip.compressGZIP(_responseBody.getBytes());
//                else
                    _responseRawBody = _responseBody.getBytes();
                return _responseRawBody;
            }
            try {
                _responseRawBody = Files.readAllBytes(event.getResponseBodyFile(taskIndex).toPath());
//                if (_responseHeaders.isZipped())
//                    _responseBody = Gzip.decompressGZIPToString(_responseRawBody);
//                else
                    _responseBody = new String(_responseRawBody);
            } catch (Exception e) {

            }
        }
        if (_responseBody == null && _responseRawBody != null)
            _responseBody = new String(_responseRawBody);
        return _responseRawBody;
    }

    @Override
    public String getDescription() {
        if (_description == null) {
            try {
                _description = new String(Files.readAllBytes(event.getResponseBodyFile(taskIndex).toPath()));
            } catch (Exception e) {

            }
        }
        return _description;
    }

    @Override
    public String getResponseBodyAsString() {
        getResponseBody();
        return _responseBody;
    }

}
