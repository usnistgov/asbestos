package gov.nist.asbestos.asbestosProxy.events;


import gov.nist.asbestos.http.headers.Headers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * An EventStore is a request (the trigger) and any number of tasks undertaken
 * to satisfy that request.
 */
public class EventStore {
    SimStore simStore;
    File root;
    File _request = null; // interaction with client
    List<File> _tasks = new ArrayList<>(); // downstream/backend interactions
    File current = null; // either request or a task

    Event e = null;

    private void clearCache() {
        e = new Event();
    }

    public Event newEvent() {
        e = new Event(this, simStore.getChannelId(), simStore.getResource(), simStore.getEventId());
        if (!e.isComplete())
            throw new Exception("Trying to create new event without details.");
        return e;
    }

    public EventStore(SimStore simStore, File eventDir) {
        this.simStore = simStore;
        this.root = eventDir;
        int i = 0;
        while (true) {
            File taskFile = getTaskFile(i);
            if (taskFile.exists())
                _tasks[i] = taskFile;
            else
                break;
            i++;
        }
        clearCache();
    }

    /**
     * creates request (if doesn't exist) and sets it as current
     * @return the request dir
     */
    public File getRequest() {
        if (_request == null) {
            _request = new File(root, "request");
            _request.mkdir();
        }
        current = _request;
        clearCache();
        return _request;
    }

    public File getTaskFile(int i) {
        return new File(root, "task${i}");
    }

    /**
     * creates new task and sets it as current
     * @return the task dir
     */
    public Task newTask() {
        int i = _tasks.size();
        File task = getTaskFile(i);
        task.mkdir();
        current = task;
        _tasks.add(task);
        clearCache();
        return new Task(this, i);
    }

    public int getTaskCount() {
        _tasks.size();
    }

    /**
     * Select a task as current
     * @param i selectTask(-1) is the same as selectRequest() except for the return type
     */
    public Task selectTask(int i) {
        if (i >= getTaskCount())
            throw new Exception("EventStore: cannot return task #${i} - only ${taskCount} tasks\n");
        if (i < 0)
            current = _request;
        else
            current = _tasks[i];
        clearCache();
        return new Task(this, i);
    }

    public Task selectClientTask() {
        return selectTask(-1);
    }

    /**
     * select request as current
     * @return
     */
    public EventStore selectRequest() {
        if (_request == null)
            return getRequest();
        current = _request;
        clearCache();
        return this;
    }


    private File getRequestHeaderFile() { return new File(current, "request_header.txt"); }
    private File getRequestBodyFile() { return new File(current, "request_body.bin"); }
    private File getRequestBodyStringFile() {  return new File(current, "request_body.txt"); }
    private File getResponseHeaderFile() {  return new File(current, "response_header.txt"); }
    private File getResponseBodyFile() {  return new File(current, "response_body.bin"); }
    private File getResponseBodyStringFile() {  return new File(current, "response_body.txt"); }
    private File getResponseBodyHTMLFile() {  return new File(current, "response_body.html"); }
    private File getRequestBodyHTMLFile() {  return new File(current, "request_body.html"); }

    public void putRequestHeader(Headers headers) {
        e._requestHeaders = headers;
        current.mkdirs();
        try (PrintWriter out = new PrintWriter(getRequestHeaderFile())) {
            out.print(headers.toString());
        }
    }

    public void putRequestBody(byte[] body) {
        e._requestRawBody = body;
        e._requestBody = new String(body);
        current.mkdirs();
        if (body.length > 0) {
            try (FileOutputStream stream = new FileOutputStream(getRequestBodyFile())) {
                stream.write(body);
            }
        }
    }

    Headers getRequestHeader() {
        if (e._requestHeaders == null) {
            String headerString = new String(Files.readAllBytes(getRequestHeaderFile().toPath()));
            e._requestHeaders = new Headers(headerString);
        }
        return e._requestHeaders;
    }

    public byte[] getRequestBody() {
        if (e._requestRawBody == null) {
            e._requestRawBody = Files.readAllBytes(getRequestBodyFile().toPath());
            e._requestBody = new String(e._requestRawBody);
        }
        return e._requestRawBody;
    }

    public String getRequestBodyAsString() {
        getRequestBody();
        return e._requestBody;
    }

    public void putResponseHeader(Headers headers) {
        e._responseHeaders = headers;
        current.mkdirs();
        try (PrintWriter out = new PrintWriter(getResponseHeaderFile())) {
            out.print(headers.toString());
        }
    }

    public void putResponseBody(byte[] body) {
        e._responseRawBody = body;
        current.mkdirs();
        if (body.length >  0) {
            try (FileOutputStream out = new FileOutputStream(getResponseBodyFile())) {
                out.write(body);
            }
        }
    }

    public void putResponseBodyText(String body) {
        e._responseBody = body;
        current.mkdirs();
        try (PrintWriter out = new PrintWriter(getResponseBodyStringFile())) {
            out.print(body);
        }
    }

    public void putRequestBodyText(String body) {
        current.mkdirs();
        try (PrintWriter out = new PrintWriter(getRequestBodyStringFile())) {
            out.print(body);
        }
    }

    public void putResponseHTMLBody(byte[] body) {
        current.mkdirs();
        putResponseBody(body);
        String bodyString = new String(body);
        putResponseBodyText(bodyString);
        try (PrintWriter out = new PrintWriter(getResponseBodyHTMLFile())) {
            out.print(bodyString);
        }
    }

    void putRequestHTMLBody(byte[] body) {
        current.mkdirs();
        putRequestBody(body);
        String bodyString = new String(body);
        putRequestBodyText(bodyString);
        try (PrintWriter out = new PrintWriter(getRequestBodyHTMLFile())) {
            out.print(bodyString);
        }
    }

    public Headers getResponseHeader() {
        if (e._responseHeaders == null) {
           String resp = new String(Files.readAllBytes(getResponseHeaderFile().toPath()));
            e._responseHeaders = new Headers(resp);
        }
        return e._responseHeaders;
    }

    public byte[] getResponseBody() {
        if (e._responseRawBody == null) {
            e._responseRawBody = Files.readAllBytes(getResponseBodyFile().toPath());
            e._responseBody = new String(e._responseRawBody)
        }
        return e._responseRawBody;
    }

    public String getResponseBodyAsString() {
        getResponseBody();
        return e._responseBody;
    }

}
