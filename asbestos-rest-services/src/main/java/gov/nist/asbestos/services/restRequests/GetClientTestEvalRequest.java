package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static Logger log = Logger.getLogger(GetClientTestEvalRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return  (request.uriParts.size() == 7 || request.uriParts.size() == 8) &&
                request.uriParts.get(3).equals("clienteval")
                ;
    }

    public GetClientTestEvalRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
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

    public void run() throws IOException {
        request.announce("GetClientTestEval");
        request.parseChannelName(4);
        String testCollection = request.uriParts.get(6);
        int eventsToEvaluate;


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

        List<Event> events = getEvents(simId, eventsToEvaluate);

        File testLogDir = request.ec.getTestLogCollectionDir(request.fullChannelId(), testCollection);

        // for one testId
        String testId = request.uriParts.get(7);

        evalClientTest(testCollection, testDirs, testSession, events);

        String myStr = saveLog(testLogDir, testId);
        request.returnString(myStr);
    }

    private String saveLog(File testLogDir, String testId) throws IOException {
        StringBuilder buf = buildJson(testId);

        String myStr = buf.toString();
        Files.write(Paths.get(new File(testLogDir, testId + ".json").toString()), myStr.getBytes());
        return myStr;
    }

    public List<Event> getEvents(SimId simId, int limit) {
        List<File> eventDirsSinceMarker = request.ec.getEventsSince(simId, null);
        eventDirsSinceMarker.sort(Comparator.comparing(File::getName).reversed());
        List<Event> events = eventDirsSinceMarker.stream().map(Event::new).sorted().collect(Collectors.toList());
        Collections.reverse(events);
        return events.stream().limit(limit).collect(Collectors.toList());
    }

    List<Event> selectedEvents = new ArrayList<>();
    Result result = new Result();

    // testDirs always has single entry
    // returns JSON response to client
    public void evalClientTest(String testCollectionId, List<File> testDirs, String testSession, List<Event> events) {
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
                if (event.isSupportEvent())
                    continue;
                selectedEvents.add(eventCount, event);
                eventCount++;
                try {
                    ModularEngine modularEngine = new ModularEngine(testDir, testScript);
                    FixtureMgr fm = modularEngine.getFixtureMgr();
                    fm.setTestId(theTestId);
                    fm.setTestCollectionId(testCollectionId);
                    modularEngine.setTestId(theTestId);
                    modularEngine.setTestCollection(testCollectionId);
                    modularEngine.setChannelId(request.fullChannelId());
                    modularEngine.addCache(testCollectionsBase);
                    modularEngine.setVal(new Val());
                    modularEngine.setTestSession(testSession);
                    modularEngine.setExternalCache(request.externalCache);
                    modularEngine.setModularScripts();
                    boolean skipThisEvent = isInternalRequestUrl(event)
                            || ITask.HTTP_VERB_GET.equals(event.getClientTask().getVerb()); // GET request has no resource request body
                    ResourceWrapper requestResource = (skipThisEvent) ? null : getRequestResource(event);
                    ResourceWrapper responseResource = (skipThisEvent) ? null : getResponseResource(event);
                    if (!skipThisEvent && ( requestResource == null || responseResource == null)) {
                        log.log(Level.SEVERE, "Request or Response cannot be Null for EventId %s. Check contentType in respective HTTP Header(s)." + event.getEventId());
                    } else {
                        modularEngine.runEval(requestResource, responseResource, skipThisEvent);
                        EventResult eventResult = result.results.get(theTestId);
                        if (eventResult == null)
                            eventResult = new EventResult();
                        List<TestReport> thisReports = modularEngine.getTestReports();
                        eventResult.reports.put(event.getEventId(), thisReports);
                        if (!thisReports.isEmpty()) {
                            if (thisReports.get(0).getResult().toCode().equalsIgnoreCase("pass")) {
                                if (skipThisEvent) {
                                    // Reset eval status to Null because this event is not a qualifying event
                                    thisReports.get(0).setResult(TestReport.TestReportResult.NULL);
                                } else {
                                    testGoodCount++;
                                    lastGoodEvent = event.getEventId();
                                }
                            }
                        }
                        result.results.put(theTestId, eventResult);
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, t.toString(), t);
                    throw t;
                }
            }
            if (testGoodCount > 0)
                log.log(Level.FINE, "Test has passing Events.");
        }
    }

    private static boolean isInternalRequestUrl(Event event) {
        return event.getClientTask().getRequestHeader().get(Headers.X_FTK_URL) != null;
    }


    /**
     * Request
     * https://hl7.org/fhir/http.html#mime-type
     * The correct mime type SHALL be used by clients and servers.
     * @param event
     * @return
     */
    private ResourceWrapper getRequestResource(Event event) {
        ITask task = event.getClientTask();

        try {
            String requestContentType = event.getClientTask().getRequestHeader().getContentType().getValue();
            Format format = Format.fromContentType(requestContentType);
            if (format == null) {
                log.log(Level.SEVERE, String.format("Request header content-type is missing for EventId %s.", event.getEventId()));
                return null;
            }
            String requestString = task.getRequestBodyAsString();
            ResourceWrapper wrapper;
            if (requestString == null) {
                wrapper = new ResourceWrapper();
            } else {
                BaseResource resource = ParserBase.parse(requestString, format);
                wrapper = new ResourceWrapper(resource);
            }
            HttpBase base = task.getHttpBase();
            wrapper.setHttpBase(base);
            return wrapper;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Response
     * https://hl7.org/fhir/http.html#mime-type
     * The correct mime type SHALL be used by clients and servers.
     * @param event
     * @return
     */
    private ResourceWrapper getResponseResource(Event event) {
        ITask task = event.getClientTask();

        try {
            String responseString = event.getClientTask().getResponseBodyAsString();
            String responseContentType = event.getClientTask().getResponseHeader().getContentType().getValue();

            Format rformat = Format.fromContentType(responseContentType);
            if (rformat == null) {
                log.log(Level.SEVERE, "Response header content-type is missing for EventId %s." + event.getEventId());
                return null;
            }
            ResourceWrapper wrapper;
            if (responseString == null) {
                wrapper = new ResourceWrapper();
            } else {
                BaseResource rresource = ParserBase.parse(responseString, rformat);
                wrapper = new ResourceWrapper(rresource);
            }
            HttpBase base = task.getHttpBase();
            wrapper.setHttpBase(base);
            return wrapper;
        } catch (Throwable t) {
            System.err.println("Event " + event.getEventId() + ":");
            t.printStackTrace();
            throw new RuntimeException(t);
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
                    buf.append(ParserBase.encode(testReport, Format.JSON));
                }
                buf.append("]\n");
            }
            buf.append("\n  }\n");
        }

        buf.append('}');
        return buf;
    }
}
