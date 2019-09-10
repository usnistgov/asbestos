package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProxyLogServlet extends HttpServlet {
    private File externalCache = null;
    private static Logger log = Logger.getLogger(ProxyLogServlet.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // TODO put EC location in web.xml
        setExternalCache(new File("/home/bill/ec"));
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String uri = req.getRequestURI();
        log.info("doPost " + uri);

        Headers headers = Common.getRequestHeaders(req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean htmlOk = acceptHeader.getValue().contains("text/html");
        boolean jsonOk = acceptHeader.getValue().contains("json");

        // uri should be
        // testSession/channelId/resourcetype/event
        //
        // 6 - event
        // 5 - resourceType - may be null
        // 4 - channelId
        // 3 - testSession

        String[] uriParts = uri.split("/");
//        if (uriParts.length == 9) { // part
//            buildJsonListingOfPart(resp, uriParts[3], uriParts[4], uriParts[5], uriParts[6], uriParts[7], uriParts[8]);
//            return;
//        }
//        if (uriParts.length == 8) { // task
//            buildJsonListingOfTask(resp, uriParts[3], uriParts[4], uriParts[5], uriParts[6], uriParts[7]);
//            return;
//        }
        if (uriParts.length == 7) {  // includes event
            if (jsonOk) {
                buildJsonListingOfEvent(resp, uriParts[3], uriParts[4], uriParts[5], uriParts[6]);
                return;
            } else if (htmlOk) {
                buildFullHtmlListing(resp, uriParts);
                return;
            }
        }
        if (uriParts.length == 6 && jsonOk) {  // includes resourceType
            // JSON listing of events in resourceType
            buildJsonListingOfEvents(resp, uriParts[3], uriParts[4], uriParts[5]);
            return;
        }
        if (uriParts.length == 5 && jsonOk) {  // includes channelId
            String query = req.getQueryString();
            if (query != null && query.contains("summaries=true")) {
                buildJsonListingOfEventSummaries(resp, uriParts[3], uriParts[4]);
                return;
            }
            // JSON listing of resourceTypes in channelId
            buildJsonListingOfResourceTypes(resp, uriParts[3], uriParts[4]);
            return;
        }

        resp.setStatus(resp.SC_BAD_REQUEST);
    }

    private void buildJsonListingOfResourceTypes(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = fhirDir(testSession, channelId);

        List<String> resourceTypes = dirListingAsList(fhir);
        returnJsonList(resp, resourceTypes);
    }

    private File fhirDir(String testSession, String channelId) {
        File psimdb = new File(externalCache, "psimdb");
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        return new File(channelFile, "fhir");
    }

    private void buildJsonListingOfEvents(HttpServletResponse resp, String testSession, String channelId, String resourceType) {
        File fhir = fhirDir(testSession, channelId);
        File resourceTypeFile = new File(fhir, resourceType);

        List<String> events = dirListingAsList(resourceTypeFile);
        returnJsonList(resp, events);
    }

    private List<String> dirListingAsList(File dir) {
        List<String> contents = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                contents.add(file.getName());
            }
            contents = contents.stream().sorted().collect(Collectors.toList());
        }

        return contents;
    }

    class Task {
        int index;
        String label;
        String description;
        String requestHeader;
        String requestBody;
        String responseHeader;
        String responseBody;

        Task(File eventDir, String taskLabel) {
            description = read(eventDir, taskLabel, "description.txt");
            requestHeader = read(eventDir, taskLabel, "request_header.txt");
            requestBody = read(eventDir, taskLabel, "request_body.txt");
            responseHeader = read(eventDir, taskLabel, "response_header.txt");
            responseBody = read(eventDir, taskLabel, "response_body.txt");
        }
    }

    class EventSummary {
        String eventName;
        String resourceType;
        String verb;
        boolean status;

        EventSummary(File eventFile) {
            try {
                String responseHeader = read(eventFile, "task0", "response_header.txt");
                Headers headers = new Headers(responseHeader);
                status = headers.getStatus() < 202;
                String requestHeader = read(eventFile, "task0", "request_header.txt");
                headers = new Headers(requestHeader);
                verb = headers.getVerb();
            } catch (Exception e) {
                status = false;
            }
        }
    }

    class Event {
        String eventName;
        String resourceType;
        List<Task> tasks = new ArrayList<>();

        Event(File eventDir) {
            List<String> parts = dirListingAsList(eventDir);
            int i = 0;
            for (String part : parts) {
                Task task = new Task(eventDir, part);
                task.label = part;
                task.index = i++;
                tasks.add(task);
            }
        }
    }

    private void buildJsonListingOfEventSummaries(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = fhirDir(testSession, channelId);
        List<String> resourceTypes = dirListingAsList(fhir);
        List<EventSummary> eventSummaries = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            File resourceDir = new File(fhir, resourceType);
            List<String> eventIds = dirListingAsList(resourceDir);
            for (String eventId : eventIds) {
                File eventFile = new File(resourceDir, eventId);
                EventSummary summary = new EventSummary(eventFile);
                summary.resourceType = resourceType;
                summary.eventName = eventId;
                eventSummaries.add(summary);
            }
        }
        String json = new Gson().toJson(eventSummaries);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    private void buildJsonListingOfEvent(HttpServletResponse resp, String testSession, String channelId, String resourceType, String eventName) {
        File fhir = fhirDir(testSession, channelId);
        if (resourceType.equals("null")) {
            resourceType = resourceTypeForEvent(fhir, eventName);
            if (resourceType == null) {
                resp.setStatus(resp.SC_NOT_FOUND);
                return;
            }
        }
        File resourceTypeFile = new File(fhir, resourceType);
        File eventDir = new File(resourceTypeFile, eventName);

        Event event = new Event(eventDir);
        event.eventName = eventName;
        event.resourceType = resourceType;

        String json = new Gson().toJson(event);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    private String resourceTypeForEvent(File fhir, String eventName) {
        File[] resourceTypeFiles = fhir.listFiles();
        if (resourceTypeFiles != null) {
            for (File resourceTypeDir : resourceTypeFiles) {
                File[] eventFiles = resourceTypeDir.listFiles();
                if (eventFiles != null) {
                    for (File eventFile : eventFiles) {
                        if (eventFile.getName().equals(eventName)) {
                            return resourceTypeDir.getName();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void returnJsonList(HttpServletResponse resp, List<String> theList) {
        String json = new Gson().toJson(theList);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    private void buildFullHtmlListing(HttpServletResponse resp, String[] uriParts) {
        String testSession = uriParts[3];
        String channelId = uriParts[4];
        String resourcetype = uriParts[5];
        String event = uriParts[6];

        File psimdb = new File(externalCache, "psimdb");
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        File fhir = new File(channelFile, "fhir");
        File resourceType = new File(fhir, resourcetype);
        File theEvent = new File(resourceType, event);

        if (!theEvent.exists() || !theEvent.canRead() || !theEvent.isDirectory()) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return;
        }

        //resp.addHeader("Content-Type", "text/html; charset=utf-8");
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE HTML>\n<html><body>");

        b.append("<h1>" + event + "</h1>");

        for (int task=0; ; task++) {
            File taskDir = new File(theEvent, "task" + task);
            if (!taskDir.exists())
                break;
            displayEvent(b, theEvent, "Task" + task);
        }

        b.append("</body></html>");

        try {
            resp.getOutputStream().write(b.toString().getBytes());
        } catch (IOException e) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void displayEvent(StringBuilder b, File theEvent, String label) {
        String section = label.toLowerCase();
        b.append("<h2>").append(label).append("</h2>");
        String description = read(theEvent, section, "description.txt");
        if (!description.equals("")) {
            b.append("<h4>Description</h4>");
            b.append("<pre>").append(description).append("</pre>");
        }
        b.append("<h3>Request</h3>");
        b.append("<pre>").append(read(theEvent, section, "request_header.txt")).append("</pre>");
        b.append("<pre>").append(read(theEvent, section, "request_body.txt")).append("</pre>");

        b.append("<h3>Response</h3>");
        b.append("<pre>").append(read(theEvent, section, "response_header.txt")).append("</pre>");
        b.append("<pre>").append(read(theEvent, section, "response_body.txt")).append("</pre>");
    }

    private String read(File theEvent, String theSection, String thePart) {
        File file = new File(new File(theEvent, theSection), thePart);
        if (!file.exists() || !file.canRead()) {
            String fileSt = file.toString();
            if (fileSt.endsWith(".txt")) {
                fileSt = fileSt.replace(".txt", ".bin");
                file = new File(fileSt);
            }
            if (!file.exists() || !file.canRead()) {
                return "";
            }
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            content = content.replaceAll("<", "&lt;");
            return content;
        } catch (Exception e) {
            ;
        }
        return "";
    }

}
