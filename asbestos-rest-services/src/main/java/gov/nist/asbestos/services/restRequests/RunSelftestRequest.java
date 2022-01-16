package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "selftest"
// 4 - channelName (testSession__channelId)
// 5 - testCollectionId
// 6 - "run" or "status"
// 7 - number of events to evaluate (client tests only)

// For server tests:
// "run"  reruns the tests and returns status of last run
// "status" just returns status of last run

// For client tests, always runs

// return is LastTime class
// and 200 status

import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.net.URI;
import java.util.List;

public class RunSelftestRequest {
    private static Logger log = Logger.getLogger(RunSelftestRequest.class.getName());

    private Request request;

    static class LastTime {
        String time = null;
        boolean allRun = true;
        boolean hasError = false;
        boolean noRuns = false;
    }

    public static boolean isRequest(Request request) {
        return (request.uriParts.size() == 7 || request.uriParts.size() == 8)  && request.uriParts.get(3).equals("selftest");
    }

    public RunSelftestRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() {
        String channelName = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        boolean isRun = request.uriParts.get(6).equals("run");
        request.announce("RunSelftestRequest");

        LastTime lastTime = new LastTime();

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelName);
        if (channelConfig == null) {
            request.badRequest("Channel not found");
            return;
        }

        boolean client = request.ec.isClientTestCollection(testCollection);

        if (client) {
            List<File> testDirs = request.ec.getTests(testCollection);
            String testSession = channelConfig.getTestSession();
            int eventsToEvaluate = client ? Integer.parseInt(request.uriParts.get(7)) : 0;
            SimId simId = SimId.buildFromRawId(channelName);

            GetClientTestEvalRequest clientEval = new GetClientTestEvalRequest(request);
            List<Event> events = clientEval.getEvents(simId);

            clientEval.evalClientTest(testDirs, testSession, events, eventsToEvaluate);

            GetClientTestEvalRequest.Summary summary = clientEval.buildSummary();
            lastTime.time = summary.time;
            lastTime.hasError = !summary.allPass;
            request.returnObject(lastTime);
            request.resp.setStatus(request.resp.SC_OK);
        } else if (isRun) {
            URI proxy = channelConfig.proxyURI();

            File patientCacheDir = request.ec.getTestLogCacheDir(channelName);
            File alternatePatientCacheDir = request.ec.getTestLogCacheDir("default__default");
            patientCacheDir.mkdirs();
            alternatePatientCacheDir.mkdirs();

            FhirClient fhirClient = new FhirClient()
                    .setFormat(request.isJson ? Format.JSON : Format.XML)
                    .sendGzip(request.isGzip)
                    .requestGzip(request.isGzip);
            TestReport report;
            ModularEngine modularEngine;

            List<String> testNames = request.ec.getTestsInCollection(testCollection);
            for (String testName : testNames) {
                File testDir = request.ec.getTest(testCollection, testName);
                try {
                    modularEngine = new ModularEngine(testDir, proxy).setSaveLogs(true);
                    report = modularEngine
                            //.getLastTestEngine()
                            .setTestSession(channelConfig.getTestSession())
                            .setChannelId(channelConfig.asChannelId())
                            .setExternalCache(request.externalCache)
                            .setVal(new Val())
                            .setFhirClient(fhirClient)
                            .setTestCollection(testCollection)
                            .addCache(patientCacheDir)
                            .addCache(alternatePatientCacheDir)
                            .setModularScripts()
                            .runTest()
                            .getTestReport();

                    if (report.getResult() == TestReport.TestReportResult.FAIL)
                        lastTime.hasError = true;

                    String time = report.getIssued().toString();
                    if (lastTime.time == null)
                        lastTime.time = time;
                    else if (time.compareTo(lastTime.time) < 0)
                        lastTime.time = time;

                } catch (Throwable t) {
                    log.log(Level.SEVERE, t.toString(), t);
                    lastTime.hasError = true;
                }
            }
            request.returnObject(lastTime);
            request.ok();
        } else {  // status
            List<File> testLogFiles = request.ec.getTestLogs(channelName, testCollection);
            boolean aRun = false;
            for (File testLogFile : testLogFiles) {
                if (!testLogFile.exists()) {
                    lastTime.allRun = false;
                    continue;
                }
                aRun = true;
                TestReport report = null;
                try {
                    report = (TestReport) ParserBase.parse(testLogFile);
                } catch (ca.uhn.fhir.parser.DataFormatException e) {
                    lastTime.hasError = true;
                }
                if (!lastTime.hasError) {
                    if (report.getResult() == TestReport.TestReportResult.FAIL)
                        lastTime.hasError = true;
                    String time = report.getIssued().toString();
                    if (lastTime.time == null)
                        lastTime.time = time;
                    else if (time.compareTo(lastTime.time) < 0)
                        lastTime.time = time;
                }
            }
            lastTime.noRuns = !aRun;
            if (lastTime.noRuns)
                lastTime.allRun = false;
            request.returnObject(lastTime);
            request.ok();
        }
    }

}
