package gov.nist.asbestos.asbestosProxy.requests;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "selftest"
// 4 - channelName (testSession__channelId)
// 5 - testCollectionId
// 6 - "run" or "status"

// "run"  reruns the tests
// "status" returns status of last run

// returns LastTime class
// and 200 status

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.net.URI;
import java.util.List;

public class RunSelftestRequest {
    private static Logger log = Logger.getLogger(RunSelftestRequest.class);

    private Request request;

    static class LastTime {
        String time = null;
        boolean allRun = true;
        boolean hasError = false;
        boolean noRuns = false;
    }

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("selftest");
    }

    public RunSelftestRequest(Request request) {
        this.request = request;
    }

    public void run() {
        String channelName = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        boolean isRun = request.uriParts.get(6).equals("run");
        log.info("Selftest - " + testCollection);

        LastTime lastTime = new LastTime();

        ChannelConfig channelConfig;
        try {
            channelConfig =  ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelName);
        } catch (Throwable e) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            return;
        }
        if (channelConfig == null) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            return;
        }

        if (isRun) {
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
                            .setChannelId(channelConfig.getTestSession() + "__" + channelConfig.getChannelId())
                            .setExternalCache(request.externalCache)
                            .setVal(new Val())
                            .setFhirClient(fhirClient)
                            .setTestCollection(testCollection)
                            .addCache(patientCacheDir)
                            .addCache(alternatePatientCacheDir)
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
                    log.error(ExceptionUtils.getStackTrace(t));
                    lastTime.hasError = true;
                }
            }
            Returns.returnObject(request.resp, lastTime);
            request.resp.setStatus(request.resp.SC_OK);
        } else {  // status
            List<File> testLogFiles = request.ec.getTestLogs(channelName, testCollection);
            boolean aRun = false;
            for (File testLogFile : testLogFiles) {
                if (!testLogFile.exists()) {
                    lastTime.allRun = false;
                    continue;
                }
                aRun = true;
                TestReport report = (TestReport) ProxyBase.parse(testLogFile);
                if (report.getResult() == TestReport.TestReportResult.FAIL)
                    lastTime.hasError = true;
                String time = report.getIssued().toString();
                if (lastTime.time == null)
                    lastTime.time = time;
                else if (time.compareTo(lastTime.time) < 0)
                    lastTime.time = time;
            }
            lastTime.noRuns = !aRun;
            if (lastTime.noRuns)
                lastTime.allRun = false;
            Returns.returnObject(request.resp, lastTime);
            request.resp.setStatus(request.resp.SC_OK);
        }
    }
}
