package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "clienteval"
// 4 - channelName   testSession__channelId
// 5 - "marker" or number of events to evaluate
// 6 - testCollectionId
// 7 - testId (optional)
// Run a client test

public class GetClientTestEvalRequest {
    private static Logger log = Logger.getLogger(GetClientTestEvalRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return  request.uriParts.get(3).equals("clienteval") &&
                (request.uriParts.size() == 7 || request.uriParts.size() == 8);
    }

    public GetClientTestEvalRequest(Request request) {
        this.request = request;
    }

    class Result {
        Map<String, EventResult> results = new HashMap<>(); // testId -> EventResult
    }

    class EventResult {
        Map<String, TestReport> reports = new HashMap<>(); // eventId -> TestReport
    }

    public void run() {
        log.info("GetClientTestEval");
        request.parseChannelName(4);
        String testCollection = request.uriParts.get(6);
        boolean useMarker = false;
        int eventsToEvaluate = 0;

        if (request.uriParts.get(5).equals("marker"))
            useMarker = true;
        else
            eventsToEvaluate = Integer.parseInt(request.uriParts.get(5));

        List<File> testDirs = new ArrayList<>();
        if (request.uriParts.size() == 7)  // no testID specified - do all
            testDirs = request.ec.getTests(testCollection);
        else { // testId specified - do one
            File dir = request.ec.getTest(testCollection, request.uriParts.get(7));
//            if (dir == null) {
//                request.resp.setStatus(request.resp.SC_NOT_FOUND);
//                return;
//            }
            if (dir != null)
                testDirs = Collections.singletonList(dir);
        }

        Map<String, File> testIds = testDirs.stream().collect(Collectors.toMap(File::getName, x -> x));
        // testId -> testScript
        Map<String, TestScript> testScripts = testDirs.stream().collect(
                Collectors.toMap(File::getName, TestEngine::loadTestScript)
        );

        String marker = useMarker ?
                request.ec.getLastMarker(request.testSession, request.channelId)
                : null;
        SimId simId = SimId.buildFromRawId(request.uriParts.get(4));
        String testSession = simId.getTestSession().getValue();

        List<File> eventDirsSinceMarker = request.ec.getEventsSince(simId, marker);
        eventDirsSinceMarker.sort(Comparator.comparing(File::getName).reversed());
        List<Event> events = eventDirsSinceMarker.stream().map(Event::new).sorted().collect(Collectors.toList());
        Collections.reverse(events);
        if (!useMarker)
            events = events.subList(0, Math.min(events.size(), eventsToEvaluate));

        Map<Event, ResourceWrapper> requestResources = new HashMap<>();
        Map<Event, ResourceWrapper> responseResources = new HashMap<>();
        for (Event event : events) {
            ITask task = event.getClientTask();
            String verb = task.getVerb();
            try {
                String requestString = task.getRequestBodyAsString();
                if (requestString != null) {
                    String requestContentType = event.getClientTask().getRequestHeader().getContentType().getValue();
                    Format format = Format.fromContentType(requestContentType);
                    BaseResource resource = ProxyBase.parse(requestString, format);
                    ResourceWrapper wrapper = new ResourceWrapper(resource);
                    HttpBase base = verb.equalsIgnoreCase("post") ? new HttpPost() : new HttpGet();
                    task.fromTask(base);
                    wrapper.setHttpBase(base);
                    requestResources.put(event, wrapper);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            try {
                String responseString = event.getClientTask().getResponseBodyAsString();
                if (responseString != null) {
                    String responseContentType = event.getClientTask().getResponseHeader().getContentType().getValue();
                    Format rformat = Format.fromContentType(responseContentType);
                    BaseResource rresource = ProxyBase.parse(responseString, rformat);
                    ResourceWrapper wrapper = new ResourceWrapper(rresource);
                    HttpBase base = verb.equalsIgnoreCase("post") ? new HttpPost() : new HttpGet();
                    task.fromTask(base);
                    wrapper.setHttpBase(base);
                    responseResources.put(event, wrapper);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        Result result = new Result();
        for (String testId : testIds.keySet()) {
            File testDir = testIds.get(testId);
            TestScript testScript = testScripts.get(testId);
            for (Event event : events) {
                TestEngine testEngine = new TestEngine(testDir, testScript);
                testEngine.setVal(new Val());
                testEngine.setTestSession(testSession);
                testEngine.setExternalCache(request.externalCache);
                ResourceWrapper responseResource = responseResources.get(event);
                testEngine.runEval(requestResources.get(event), responseResources.get(event));
                EventResult eventResult = result.results.get(testId); //new EventResult();
                if (eventResult == null)
                    eventResult = new EventResult();
                eventResult.reports.put(event.getEventId(), testEngine.getTestReport());
                result.results.put(testId, eventResult);
            }
        }

        // for one testId
        String testId = request.uriParts.get(7);

        StringBuilder buf = new StringBuilder();
        buf.append('{').append('"').append(testId).append('"').append(':').append("\n ");

        File testLogDir = request.ec.getTestLogDir(request.fullChannelId(), testCollection);
        EventResult er = result.results.get(testId);
        if (er == null || er.reports == null || er.reports.isEmpty())
            buf.append("null");
        else {
            buf.append(" {\n");
            boolean first = true;
            for (String eventId : er.reports.keySet()) {
                if (first)
                    first = false;
                else
                    buf.append(',');

                buf.append('"').append(eventId).append('"').append(":\n ");
                TestReport testReport = er.reports.get(eventId);
                buf.append(ProxyBase.encode(testReport, Format.JSON));
            }
            buf.append("\n  }\n");
        }

        buf.append('}');

        String myStr = buf.toString();
        try {
            Files.write(Paths.get(new File(testLogDir, testId + ".json").toString()), myStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Returns.returnString(request.resp, myStr);
    }
}
