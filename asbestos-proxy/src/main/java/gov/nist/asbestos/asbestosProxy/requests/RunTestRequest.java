package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestReport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testrun"
// 4 - channelName (testSession__channelId)
// 5 - testCollectionId
// 6 - testId

public class RunTestRequest {
    private static Logger log = Logger.getLogger(RunTestRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testrun");
    }

    public RunTestRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("RunTest");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        String testName = request.uriParts.get(6);

        ChannelConfig channelConfig;
        try {
            channelConfig =  ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        } catch (Throwable e) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            return;
        }
        if (channelConfig == null) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            return;
        }
        String testSession = channelConfig.getTestSession();
        String proxyStr = null;
        ServicePropertiesEnum key = ServicePropertiesEnum.FHIR_TOOLKIT_BASE;
        proxyStr = ServiceProperties.getInstance().getPropertyOrStop(key);
        proxyStr += "/proxy/" + channelId;
        URI proxy = null;
        try {
            proxy = new URI(proxyStr);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        File testDir = request.ec.getTest(testCollection, testName);

        File patientCacheDir = request.ec.getTestLogCacheDir(channelId);
        File alternatePatientCacheDir = request.ec.getTestLogCacheDir("default__default");
        patientCacheDir.mkdirs();
        alternatePatientCacheDir.mkdirs();

        FhirClient fhirClient = new FhirClient()
                .setFormat(request.isJson ? Format.JSON : Format.XML)
                .sendGzip(request.isGzip)
                .requestGzip(request.isGzip);
        TestReport report;
        ModularEngine modularEngine;
        try {
            modularEngine = new ModularEngine(testDir, proxy);
            report = modularEngine
                    .getLastTestEngine()
                    .setTestSession(testSession)
                    .setChannelId(channelId)
                    .setExternalCache(request.externalCache)
                    .setVal(new Val())
                    .setFhirClient(fhirClient)
                    .setTestCollection(testCollection)
                    .addCache(patientCacheDir)
                    .addCache(alternatePatientCacheDir)
                    .runTest()
                    .getTestReport();

        } catch (Throwable t) {
            log.error(ExceptionUtils.getStackTrace(t));
            throw t;
        }
        report.setName(testName);
        String json = Returns.returnResource(request.resp, report);
        Path path = request.ec.getTestLog(channelId, testCollection, testName).toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(json);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }
}
