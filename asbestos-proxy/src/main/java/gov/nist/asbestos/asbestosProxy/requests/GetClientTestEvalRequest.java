package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

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
// 5 - testCollectionId
// 6 - testId (optional)
// Run a client test

public class GetClientTestEvalRequest {
    private static Logger log = Logger.getLogger(GetClientTestEvalRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return  request.uriParts.get(3).equals("clienteval") &&
                (request.uriParts.size() == 6 || request.uriParts.size() == 7);
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
        String testCollection = request.uriParts.get(5);

        List<File> testDirs;
        if (request.uriParts.size() == 6)  // no testID specified - do all
            testDirs = request.ec.getTests(testCollection);
        else  // testId specified - do one
            testDirs = Collections.singletonList(request.ec.getTest(testCollection, request.uriParts.get(6)));

        Map<String, File> testIds = testDirs.stream().collect(Collectors.toMap(File::getName, x -> x));
        // testId -> testScript
        Map<String, TestScript> testScripts = testDirs.stream().collect(
                Collectors.toMap(File::getName, TestEngine::loadTestScript)
        );

        String marker = request.ec.getLastMarker(request.testSession, request.channelId);
        SimId simId = SimId.buildFromRawId(request.uriParts.get(4));

        List<File> eventDirsSinceMarker = request.ec.getEventsSince(simId, marker);
        eventDirsSinceMarker.sort(Comparator.comparing(File::getName));
        List<Event> events = eventDirsSinceMarker.stream().map(Event::new).collect(Collectors.toList());

        Map<Event, BaseResource> requestResources = new HashMap<>();
        Map<Event, BaseResource> responseResources = new HashMap<>();
        for (Event event : events) {
            String requestString = event.getClientTask().getRequestBodyAsString();
            String requestContentType = event.getClientTask().getRequestHeader().getContentType().getValue();
            Format format = Format.fromContentType(requestContentType);
            BaseResource resource = ProxyBase.parse(requestString, format);
            requestResources.put(event, resource);

            String responseString = event.getClientTask().getResponseBodyAsString();
            String responseContentType = event.getClientTask().getResponseHeader().getContentType().getValue();
            Format rformat = Format.fromContentType(responseContentType);
            BaseResource rresource = ProxyBase.parse(responseString, rformat);
            responseResources.put(event, rresource);
        }

        Result result = new Result();
        for (String testId : testIds.keySet()) {
            File testDir = testIds.get(testId);
            TestScript testScript = testScripts.get(testId);
            TestEngine testEngine = new TestEngine(testDir, testScript);
            for (Event event : events) {
                BaseResource responseResource = responseResources.get(event);
                testEngine.runEval(requestResources.get(event), responseResource);
                EventResult eventResult = new EventResult();
                eventResult.reports.put(event.getEventId(), testEngine.getTestReport());
                result.results.put(testId, eventResult);
            }
        }

        Returns.returnObject(request.resp, result);
    }
}
