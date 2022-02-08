package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import java.util.logging.Logger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testrun"
// 4 - channelName (testSession__channelId)
// 5 - testCollectionId
// 6 - testId
// Returns modular test reports
//   JSON object : test/moduleId => TestReport

public class RunTestRequest {
    private static Logger log = Logger.getLogger(RunTestRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testrun");
    }

    public RunTestRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws URISyntaxException {
        request.announce("RunTest");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        String testName = request.uriParts.get(6);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) {
            request.badRequest("Channel not found");
            return;
        }
        String testSession = channelConfig.getTestSession();

        URI proxy;
        proxy = channelConfig.getProxyURI(request.isTlsProxy);

        File testDir = request.ec.getTest(testCollection, testName);

        File patientCacheDir = request.ec.getTestLogCacheDir(channelId);
        File alternatePatientCacheDir = request.ec.getTestLogCacheDir("default__default");
        patientCacheDir.mkdirs();
        alternatePatientCacheDir.mkdirs();

        FhirClient fhirClient = new FhirClient()
                .setFormat(request.isJson ? Format.JSON : Format.XML)
                .sendGzip(request.isGzip)
                .requestGzip(request.isGzip);
        ModularEngine modularEngine;
        modularEngine = new ModularEngine(testDir, proxy).setSaveLogs(true);
        modularEngine
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setExternalCache(request.externalCache)
                .setVal(new Val())
                .setFhirClient(fhirClient)
                .setTestCollection(testCollection)
                .addCache(patientCacheDir)
                .addCache(alternatePatientCacheDir)
                .setModularScripts()
                .runTest()
                .getTestReport();


        String json = modularEngine.reportsAsJson();
        request.returnString(json);
        request.ok();
    }
}
