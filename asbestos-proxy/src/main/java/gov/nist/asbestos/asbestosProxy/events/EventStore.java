package gov.nist.asbestos.asbestosProxy.events;


import gov.nist.asbestos.asbestosProxy.log.SimStore;
import gov.nist.asbestos.asbestosProxy.log.Task;
import gov.nist.asbestos.http.headers.Headers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An EventStore is a request (the trigger) and any number of tasks undertaken
 * to satisfy that request.
 */
public class EventStore {
    private SimStore simStore;
    private File root;
    private File _request = null; // interaction with client
    private ArrayList<File> _tasks = new ArrayList<>(); // downstream/backend interactions
    private File current = null; // either request or a task
    private Event e = null;

    public String toString() {
        return "EventStore: current=" + ((current == null) ? "null" : current.getPath());
    }

    private void clearCache() {
        e = new Event();
    }

    public Event newEvent() {
        e = new Event(this, simStore.getChannelId(), simStore.getResource(), simStore.getEventId());
        if (!e.isComplete())
            throw new RuntimeException("Trying to create new event without details.");
        return e;
    }

    public EventStore(SimStore simStore, File eventDir) {
        this.simStore = simStore;
        this.root = eventDir;
        int i = 0;
        while (true) {
            File taskFile = getTaskFile(i);
            if (taskFile.exists()) {
                if (_tasks.size() <= i)
                    _tasks.add(taskFile);
                else
                    _tasks.set(i, taskFile);
            }
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
    private File getRequest() {
        if (_request == null) {
            _request = new File(root, "request");
            _request.mkdir();
        }
        current = _request;
        clearCache();
        return _request;
    }

    private File getTaskFile(int i) {
        return new File(root, "task" + i);
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
        return _tasks.size();
    }

    /**
     * Select a task as current
     * @param i selectTask(-1) is the same as selectRequest() except for the return type
     */
    public Task selectTask(int i) {
        if (i >= getTaskCount())
            throw new RuntimeException("EventStore: cannot return task #${i} - only ${taskCount} tasks\n");
        if (i < 0) {
            current = getRequest();
        }
        else
            current = _tasks.get(i);
        clearCache();
        return new Task(this, i);
    }

    public Task selectClientTask() {
        return selectTask(-1);
    }

    public SimStore getSimStore() {
        return simStore;
    }

    public Event getEvent() {
        return e;
    }

//    /**
//     * select request as current
//     * @return
//     */
//    public EventStore selectRequest() {
//        if (_request == null)
//            return getRequest();
//        current = _request;
//        clearCache();
//        return this;
//    }


    public File getRequestHeaderFile() { return new File(current, "request_header.txt"); }
    public File getRequestBodyFile() { return new File(current, "request_body.bin"); }
    public File getRequestBodyStringFile() {  return new File(current, "request_body.txt"); }
    public File getResponseHeaderFile() {  return new File(current, "response_header.txt"); }
    public File getResponseBodyFile() {  return new File(current, "response_body.bin"); }
    public File getResponseBodyStringFile() {  return new File(current, "response_body.txt"); }
    public File getResponseBodyHTMLFile() {  return new File(current, "response_body.html"); }
    public File getRequestBodyHTMLFile() {  return new File(current, "request_body.html"); }

    public void putRequestHeader(Headers headers) {
        e._requestHeaders = headers;
        current.mkdirs();
        try {
            try (PrintWriter out = new PrintWriter(getRequestHeaderFile())) {
                out.print(headers.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestBody(byte[] body) {
        e._requestRawBody = body;
        if (body != null) {
            e._requestBody = new String(body);
            current.mkdirs();
            if (body.length > 0) {
                try {
                    try (FileOutputStream stream = new FileOutputStream(getRequestBodyFile())) {
                        stream.write(body);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Headers getRequestHeader() {
        if (e._requestHeaders == null) {
            String headerString;
            try {
                headerString = new String(Files.readAllBytes(getRequestHeaderFile().toPath()));
            } catch (Exception e) {
                return new Headers();
            }
            e._requestHeaders = new Headers(headerString);
        }
        return e._requestHeaders;
    }

    public byte[] getRequestBody() {
        if (e._requestRawBody == null) {
            try {
                e._requestRawBody = Files.readAllBytes(getRequestBodyFile().toPath());
            } catch (Exception e) {
            }
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
        try {
            try (PrintWriter out = new PrintWriter(getResponseHeaderFile())) {
                out.print(headers.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putResponseBody(byte[] body) {
        e._responseRawBody = body;
        current.mkdirs();
        if (body.length >  0) {
            try {
                try (FileOutputStream out = new FileOutputStream(getResponseBodyFile())) {
                    out.write(body);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void putResponseBodyText(String body) {
        e._responseBody = body;
        current.mkdirs();
        try {
            try (PrintWriter out = new PrintWriter(getResponseBodyStringFile())) {
                out.print(body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestBodyText(String body) {
        current.mkdirs();
        try {
            try (PrintWriter out = new PrintWriter(getRequestBodyStringFile())) {
                out.print(body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putResponseHTMLBody(byte[] body) {
        current.mkdirs();
        putResponseBody(body);
        String bodyString = new String(body);
        putResponseBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(getResponseBodyHTMLFile())) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putRequestHTMLBody(byte[] body) {
        current.mkdirs();
        putRequestBody(body);
        String bodyString = new String(body);
        putRequestBodyText(bodyString);
        try {
            try (PrintWriter out = new PrintWriter(getRequestBodyHTMLFile())) {
                out.print(bodyString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Headers getResponseHeader() {
        if (e._responseHeaders == null) {
            try {
                String resp = new String(Files.readAllBytes(getResponseHeaderFile().toPath()));
                e._responseHeaders = new Headers(resp);
            } catch (Exception e) {

            }
        }
        return e._responseHeaders;
    }

    public byte[] getResponseBody() {
        if (e._responseRawBody == null) {
            try {
                e._responseRawBody = Files.readAllBytes(getResponseBodyFile().toPath());
                e._responseBody = new String(e._responseRawBody);
            } catch (Exception e) {

            }
        }
        return e._responseRawBody;
    }

    public String getResponseBodyAsString() {
        getResponseBody();
        return e._responseBody;
    }

}
