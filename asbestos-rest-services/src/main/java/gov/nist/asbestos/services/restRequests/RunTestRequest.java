package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Logger;
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
    private String rawRequest;


    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testrun");
    }

    public RunTestRequest(Request request) throws IOException {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        this.rawRequest = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());  // Could be xml or json, check isJson flag
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

        final File patientCacheDir = getPatientCacheDir(channelId);
        final File catPatientCacheDir = getPatientCacheDir(EC.EXTERNAL_PATIENT_SERVER.toString());
        final File alternatePatientCacheDir = getPatientCacheDir(EC.DEFAULT.toString());

        FhirClient fhirClient = new FhirClient()
                .setFormat(request.isJson ? Format.JSON : Format.XML)
                .sendGzip(request.isGzip)
                .requestGzip(request.isGzip);
        ModularEngine modularEngine;
        modularEngine = new ModularEngine(testDir, proxy).setSaveLogs(true);

        if (request.hasUserSuppliedFixture) {
            addRequestFixture(modularEngine.getMainTestEngine().getSut(), modularEngine.getMainTestEngine().getCacheManager());
        }

        modularEngine
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setExternalCache(request.externalCache)
                .setVal(new Val())
                .setFhirClient(fhirClient)
                .setTestCollection(testCollection)
                .addCache(patientCacheDir)
                .addCache(catPatientCacheDir)
                .addCache(alternatePatientCacheDir)
                .setModularScripts()
                .runTest()
                .getTestReport();

        String json = modularEngine.reportsAsJson();
        request.returnString(json);
        request.ok();
    }

    private void addRequestFixture(URI sut, ResourceCacheMgr resourceCacheMgr) {
        Format format = (request.isJson) ? Format.JSON : Format.XML;
        BaseResource resource = ParserBase.parse(rawRequest, format);
        resourceCacheMgr.add(new Ref("urn:ftkmemory:userSuppliedTestFixture:request", sut), resource); // In memory resource cache
    }

    private File getPatientCacheDir(String channelId) {
        Objects.requireNonNull(channelId);
        File patientCacheDir = request.ec.getTestLogCacheDir(channelId);
        patientCacheDir.mkdirs();
        return patientCacheDir;
    }
}
