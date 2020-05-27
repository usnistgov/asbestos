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
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
// 5 - number of events to evaluate
// 6 - testCollectionId
// 7 - testId  (optional - if missing eval all tests in collection)
// Run a client test

public class GetClientTestEvalRequest {
    private static Logger log = Logger.getLogger(GetClientTestEvalRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return  (request.uriParts.size() == 7 || request.uriParts.size() == 8) &&
                request.uriParts.get(3).equals("clienteval")
                ;
    }

    public GetClientTestEvalRequest(Request request) {
        this.request = request;
    }

    class Summary {
        boolean allPass;
        String time;
    }

    public Summary buildSummary() {
        return result.buildSummary();
    }

    class Result {
        Map<String, EventResult> results = new HashMap<>(); // testId -> EventResult
        boolean allPass = true;
        String time;

        Summary buildSummary() {
            for (EventResult eventResult : results.values()) {
                // one iteration per testId
                eventResult.buildSummary();
                if (!eventResult.pass)
                    allPass = false;
                if (time == null)
                    time = eventResult.oldestEventTime;
                else if (time.compareTo(eventResult.oldestEventTime) < 0)
                    time = eventResult.oldestEventTime;
            }
            Summary summary = new Summary();
            summary.allPass = allPass;
            summary.time = time;
            return summary;
        }
    }

    class EventResult {
        Map<String, List<TestReport>> reports = new HashMap<>(); // eventId -> TestReport

        // summary
        String oldestEventTime;
        boolean pass;  // there is one eventId that passes

        void buildSummary() {
            oldestEventTime = null;
            pass = false;
            for (String eventId : reports.keySet()) {
                for (TestReport report : reports.get(eventId)) {
                    if (report.getResult().equals(TestReport.TestReportResult.PASS))
                        pass = true;
                    String time = report.getIssued().toString();
                    if (oldestEventTime == null)
                        oldestEventTime = time;
                    else if (time.compareTo(oldestEventTime) < 0)
                        oldestEventTime = time;
                    break;  // only look at top TestReport (final answer)
                }
            }
        }
    }

    public void run() {
        log.info("GetClientTestEval");
        request.parseChannelName(4);
        String testCollection = request.uriParts.get(6);
        int eventsToEvaluate = 0;


        eventsToEvaluate = Integer.parseInt(request.uriParts.get(5));

        List<File> testDirs = new ArrayList<>();
        if (request.uriParts.size() == 7)  // no testID specified - do all
            testDirs = request.ec.getTests(testCollection);
        else { // testId specified - do one
            File dir = request.ec.getTest(testCollection, request.uriParts.get(7));
            if (dir != null)
                testDirs = Collections.singletonList(dir);
        }

        SimId simId = SimId.buildFromRawId(request.uriParts.get(4));
        String testSession = simId.getTestSession().getValue();

        List<Event> events = getEvents(simId);

        File testLogDir = request.ec.getTestLogCollectionDir(request.fullChannelId(), testCollection);

        // for one testId
        String testId = request.uriParts.get(7);


         evalClientTest(testDirs, testSession, events, eventsToEvaluate);

        StringBuilder buf = buildJson(testId);

        String myStr = buf.toString();
        try {
            Files.write(Paths.get(new File(testLogDir, testId + ".json").toString()), myStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Returns.returnString(request.resp, myStr);
    }

    public List<Event> getEvents(SimId simId) {
        List<File> eventDirsSinceMarker = request.ec.getEventsSince(simId, null);
        eventDirsSinceMarker.sort(Comparator.comparing(File::getName).reversed());
        List<Event> events = eventDirsSinceMarker.stream().map(Event::new).sorted().collect(Collectors.toList());
        Collections.reverse(events);
        return events;
    }

    List<Event> selectedEvents = new ArrayList<>();
    Result result = new Result();

    // testDirs always has single entry
    // returns JSON response to client
    public void evalClientTest(List<File> testDirs, String testSession, List<Event> events, int eventsToEvaluate) {
        Map<String, File> testIds = testDirs.stream().collect(Collectors.toMap(File::getName, x -> x));
        //String testId = testDirs.get(0).getName();

        // testId -> testScript
        Map<String, TestScript> testScripts = testDirs.stream().collect(
                Collectors.toMap(File::getName, TestEngine::loadTestScript)
        );

        File testCollectionsBase = request.ec.getTestCollectionsBase();

        for (String theTestId : testIds.keySet()) {
            File testDir = testIds.get(theTestId);
            TestScript testScript = testScripts.get(theTestId);
            int eventCount = 0;
            // Since we may be filtering events, collect the ones that are actually
            // evaluated.
            int testGoodCount = 0;
            String lastGoodEvent = null;
            for (Event event : events) {
                if (eventCount >= eventsToEvaluate)
                    break;
                if (event.isSupportEvent())
                    continue;
                selectedEvents.add(eventCount, event);
                eventCount++;
                try {
                    ModularEngine modularEngine = new ModularEngine(testDir, testScript);
                    FixtureMgr fm = modularEngine.getFixtureMgr();
                    fm.setTestId(theTestId);
                    fm.setTestCollectionId("Inspector");
                    modularEngine.setTestId(theTestId);
                    modularEngine.setTestCollection("Inspector");
                    modularEngine.setChannelId(request.fullChannelId());
                    modularEngine.addCache(testCollectionsBase);
                    modularEngine.setVal(new Val());
                    modularEngine.setTestSession(testSession);
                    modularEngine.setExternalCache(request.externalCache);
                    ResourceWrapper requestResource = getRequestResource(event);
                    ResourceWrapper responseResource = getResponseResource(event);
                    modularEngine.runEval(requestResource, responseResource);
                    EventResult eventResult = result.results.get(theTestId); //new EventResult();
                    if (eventResult == null)
                        eventResult = new EventResult();
                    List<TestReport> thisReports = modularEngine.getTestReports();
                    eventResult.reports.put(event.getEventId(), thisReports);
                    if (!thisReports.isEmpty()) {
                        if (thisReports.get(0).getResult().toCode().equalsIgnoreCase("pass")) {
                            testGoodCount++;
                            lastGoodEvent = event.getEventId();
                        }
                    }
                    result.results.put(theTestId, eventResult);
                } catch (Throwable t) {
                    log.error(ExceptionUtils.getStackTrace(t));
                    throw t;
                }
            }
            if (testGoodCount > 0)
                System.out.println("now");
        }
    }


    private ResourceWrapper getRequestResource(Event event) {
        ITask task = event.getClientTask();

        try {
            String requestContentType = event.getClientTask().getRequestHeader().getContentType().getValue();
            Format format = Format.fromContentType(requestContentType);
            String requestString = task.getRequestBodyAsString();
            ResourceWrapper wrapper;
            if (requestString == null) {
                wrapper = new ResourceWrapper();
            } else {
                BaseResource resource = ProxyBase.parse(requestString, format);
                wrapper = new ResourceWrapper(resource);
            }
            HttpBase base = task.getHttpBase();
            wrapper.setHttpBase(base);
            return wrapper;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
    }

    private ResourceWrapper getResponseResource(Event event) {
        ITask task = event.getClientTask();

        try {
            String responseString = event.getClientTask().getResponseBodyAsString();
            String responseContentType = event.getClientTask().getResponseHeader().getContentType().getValue();
            Format rformat = Format.fromContentType(responseContentType);
            ResourceWrapper wrapper;
            if (responseString == null) {
                wrapper = new ResourceWrapper();
            } else {
                BaseResource rresource = ProxyBase.parse(responseString, rformat);
                wrapper = new ResourceWrapper(rresource);
            }
            HttpBase base = task.getHttpBase();
            wrapper.setHttpBase(base);
            return wrapper;
        } catch (Throwable t) {
            System.err.println("Event " + event.getEventId() + ":");
            t.printStackTrace();
            throw new Error(t);
        }
    }


    StringBuilder buildJson(String testId) {

        StringBuilder buf = new StringBuilder();
        buf.append('{').append('"').append(testId).append('"').append(':').append("\n ");

        EventResult er = result.results.get(testId);
        if (er == null || er.reports == null || er.reports.isEmpty())
            buf.append("null");
        else {
            buf.append(" {\n");
            boolean first = true;
            //for (String eventId : er.reports.keySet()) {
            // keep the reports in eventId order
            for (Event event : selectedEvents) {
                String eventId = event.getEventId();
                if (first)
                    first = false;
                else
                    buf.append(',');

                buf.append('"').append(eventId).append('"').append(":\n ");
                List<TestReport> testReports = er.reports.get(eventId);
                buf.append("[\n");
                boolean isFirst = true;
                for (TestReport testReport : testReports) {
                    if (!isFirst)
                        buf.append(",");
                    isFirst = false;
                    buf.append(ProxyBase.encode(testReport, Format.JSON));
                }
                buf.append("]\n");
            }
            buf.append("\n  }\n");
        }

        buf.append('}');
        return buf;
    }
}
