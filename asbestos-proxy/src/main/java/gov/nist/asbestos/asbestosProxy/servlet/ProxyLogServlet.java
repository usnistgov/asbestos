package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.asbestosProxy.channel.ChannelRelay;
import gov.nist.asbestos.asbestosProxy.event.UIEvent;
import gov.nist.asbestos.asbestosProxy.event.EventSummary;
import gov.nist.asbestos.asbestosProxy.event.Reader;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestReport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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
        String ec = (String) config.getServletContext().getAttribute("ExternalCache");
        log.info("EC is " + ec);
        setExternalCache(new File(ec));
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
    }

    boolean htmlOk;
    boolean jsonOk;
    HttpServletRequest req;
    HttpServletResponse resp;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String uri = req.getRequestURI();
        log.info("doPost " + uri);

        Headers headers = Common.getRequestHeaders(req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        htmlOk = acceptHeader.getValue().contains("text/html");
        jsonOk = acceptHeader.getValue().contains("json");

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

        // since event
        // return next event after this one (wait for it if you must)
        // wait may time out and return 404
        // when event is in hand eval against testCollectionId/testId

        // 7 - event
        // 6 - "since"
        // 5 - resourceType - may be null
        // 4 - channelId
        // 3 - testSession
        if ((uriParts.length == 8 || uriParts.length == 7) && uriParts[6].equals("since")) {
            String eventId = (uriParts.length == 8) ? uriParts[7] : null;
            String testSession = uriParts[3];
            String channelId = uriParts[4];
            waitForEvent(testSession, channelId, eventId);
            return;
        }

        // 6 - event
        // 5 - resourceType - may be null
        // 4 - channelId
        // 3 - testSession
        if (uriParts.length == 7 && jsonOk) {  // includes event
            getEvent(uriParts[3], uriParts[4], uriParts[5], uriParts[6]);
            return;
        }
        if (uriParts.length == 6 && jsonOk) {  // includes resourceType
            // JSON listing of events in resourceType
            buildJsonListingOfEvents(resp, uriParts[3], uriParts[4], uriParts[5]);
            return;
        }
        if (uriParts.length == 5 && jsonOk) {  // includes channelId
            getEvents(uriParts[3], uriParts[4]);
            return;
        }
        resp.setStatus(resp.SC_BAD_REQUEST);
    }

    class EventAndTestReport {
        UIEvent UIEvent;
        TestReport testReport;
    }

    private void waitForEvent(String testSession, String channelId, String eventId, String collectionId, String testId) {
        ChannelConfig channelConfig = ChannelControl.channelConfigFromChannelId(externalCache, channelId);
        File eventDir = eventId == null ? null : getEventDir(testSession, channelId, eventId);
        UIEvent uiEvent;
        if (eventId == null) {
            eventDir = ChannelRelay.waitForEvent(channelId);
        } else {
            // return event after eventId - wait if necessary
            List<ResourceId> ids = buildListOfEventIdsByResourceType(testSession, channelId);
            // sort ids - in date order
            for (ResourceId id : ids) {
                if (id.id.compareTo(eventId) > 0) {
                    // first event AFTER eventId timestamp
                    uiEvent = getEvent(testSession, channelId, id.id);
                    TestReport testReport = runEvaluation(testSession, channelId, collectionId, testId, eventDir);
                    returnEvent(uiEvent, testReport);
                    return;
                }
            }
            // not found in logs - wait for it
            eventDir = ChannelRelay.waitForEvent(channelId);
        }
        TestReport testReport = runEvaluation(testSession, channelId, collectionId, testId, eventDir);  // needs to be run
        returnEvent(uiEvent, testReport);
    }

    // TestScripts for client testing are called evaluations here
    private TestReport runEvaluation(String testSesssion, String channelId, String collectionId, String testId, File eventDir) {
        TestEngine testEngine = new TestEngine(
                getTestDefinition(testSesssion, channelId, collectionId, testId),
                eventDir
        );
        testEngine.runEval();
        return testEngine.getTestReport();
    }

    private File getTestDefinition(String testSesssion, String channelId, String collectionId, String testId) {
        String pathTo = String.format("%s/%s/%s", externalCache.toString(), collectionId, testId);
        return new File(pathTo);
    }

    private void returnEvent(UIEvent UIEvent, TestReport testReport) {
        EventAndTestReport eAndR = new EventAndTestReport();
        String json = new Gson().toJson(eAndR);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        resp.setStatus(resp.SC_OK);
    }

    private void getEvents(String testSession, String channelId) {
        String query = req.getQueryString();
        if (query != null && query.contains("summaries=true")) {
            buildJsonListingOfEventSummaries(resp, testSession, channelId);
            return;
        }
        // JSON listing of resourceTypes in channelId
        buildJsonListingOfResourceTypes(resp, testSession, channelId);
    }

    private void getEvent(String testSession, String channelId, String resourceType, String eventId) {
        if (jsonOk) {
            buildJsonListingOfEvent(resp, testSession, channelId, resourceType, eventId);
        } else if (htmlOk) {
            buildFullHtmlListing(testSession, channelId, resourceType, eventId);
        }
        resp.setStatus(resp.SC_BAD_REQUEST);
    }

    private void buildJsonListingOfResourceTypes(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = fhirDir(testSession, channelId);

        List<String> resourceTypes = dirListingAsStringList(fhir);
        returnJsonList(resp, resourceTypes);
    }

    private File fhirDir(String testSession, String channelId) {
        File psimdb = new File(externalCache, "psimdb");
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        return new File(channelFile, "fhir");
    }

    private class ResourceId {
        String resourceType;
        String id;

        ResourceId(String resourceType, String id) {
            this.resourceType = resourceType;
            this.id = id;
        }
    }

    private List<ResourceId> buildListOfEventIdsByResourceType(String testSession, String channelId) {
        File fhir = fhirDir(testSession, channelId);
        List<File> resourceTypes = dirListing(fhir);
        List<ResourceId> rids = new ArrayList<>();

        for (File resourceType : resourceTypes) {
            List<String> ids = dirListingAsStringList(resourceType);
            for (String id : ids) {
                ResourceId rid = new ResourceId(resourceType.getName(), id);
                rids.add(rid);
            }
        }
        return rids;
    }

    private void buildJsonListingOfEvents(HttpServletResponse resp, String testSession, String channelId, String resourceType) {
        File fhir = fhirDir(testSession, channelId);
        File resourceTypeFile = new File(fhir, resourceType);

        List<String> events = dirListingAsStringList(resourceTypeFile);
        returnJsonList(resp, events);
    }

    private List<File> dirListing(File dir) {
        List<File> contents = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                contents.add(file);
            }
            contents = contents.stream().sorted().collect(Collectors.toList());
        }

        return contents;
    }


    private void buildJsonListingOfEventSummaries(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = fhirDir(testSession, channelId);
        List<String> resourceTypes = Reader.dirListingAsStringList(fhir);
        List<EventSummary> eventSummaries = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            File resourceDir = new File(fhir, resourceType);
            List<String> eventIds = Reader.dirListingAsStringList(resourceDir);
            for (String eventId : eventIds) {
                File eventFile = new File(resourceDir, eventId);
                EventSummary summary = new EventSummary(eventFile);
                summary.setResourceType(resourceType);
                summary.setEventName(eventId);
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


    private File getEventDir(String testSession, String channelId, String eventId) {
        File fhir = fhirDir(testSession, channelId);
        String resourceType;
        resourceType = resourceTypeForEvent(fhir, eventId);
        if (resourceType == null)
            return null;

        File resourceTypeFile = new File(fhir, resourceType);
        return new File(resourceTypeFile, eventId);
    }

    private UIEvent getEvent(String testSession, String channelId, String eventId) {
        File fhir = fhirDir(testSession, channelId);
        String resourceType;
        resourceType = resourceTypeForEvent(fhir, eventId);
        if (resourceType == null)
            return null;

        File eventDir = getEventDir(testSession, channelId, eventId);

        UIEvent UIEvent = new UIEvent(eventDir);
        UIEvent.setEventName(eventId);
        UIEvent.setResourceType(resourceType);
        return UIEvent;
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

        UIEvent UIEvent = new UIEvent(eventDir);
        UIEvent.setEventName(eventName);
        UIEvent.setResourceType(resourceType);

        String json = new Gson().toJson(UIEvent);
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

    private void returnJsonList(HttpServletResponse resp, List<?> theList) {
        String json = new Gson().toJson(theList);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    private void buildFullHtmlListing(String testSession, String channelId, String resourcetype, String event) {
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
        String description = Reader.read(theEvent, section, "description.txt");
        if (!description.equals("")) {
            b.append("<h4>Description</h4>");
            b.append("<pre>").append(description).append("</pre>");
        }
        b.append("<h3>Request</h3>");
        b.append("<pre>").append(Reader.read(theEvent, section, "request_header.txt")).append("</pre>");
        b.append("<pre>").append(Reader.read(theEvent, section, "request_body.txt")).append("</pre>");

        b.append("<h3>Response</h3>");
        b.append("<pre>").append(Reader.read(theEvent, section, "response_header.txt")).append("</pre>");
        b.append("<pre>").append(Reader.read(theEvent, section, "response_body.txt")).append("</pre>");
    }


}
