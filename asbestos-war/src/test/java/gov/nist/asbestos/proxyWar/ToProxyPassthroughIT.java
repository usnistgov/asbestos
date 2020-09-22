package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ToProxyPassthroughIT {
    private static String testSession = "default";
    private static String channelId = "default";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();
    private static URI base;


    @Test
    void patientWithAutoCreate() throws URISyntaxException {
        run("/toProxy/createPatientWithAutoCreate/TestScript.xml");
    }

    @Test
    void patientWithAutoCreateDelete() throws URISyntaxException {
        run("/toProxy/createPatientWithAutoCreateDelete/TestScript.xml");
    }

    TestEngine run(String testScriptLocation) throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource(testScriptLocation).toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, base)
                .setVal(val)
                .setTestSession("default")
                .setChannelId("default__default")
                .setExternalCache(ExternalCache.getExternalCache())
                .setFhirClient(new FhirClient())
                .runTest();
        System.out.println(testEngine.getTestReportAsJson());
        TestReport report = testEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        return testEngine;
    }

    @Test
    void patientSearch() throws URISyntaxException {
        run("/toFhirServer/searchPatient/TestScript.xml");
    }

   @BeforeAll
     static void beforeAll() throws URISyntaxException {
        base = new URI(ITConfig.getChannelBase(testSession, channelId));
    }

    private static String createChannel() throws URISyntaxException, IOException {
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:" + fhirPort + "/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/asbestos/channel"), json);
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);
        return "http://localhost:" + proxyPort + "/asbestos/proxy/" + testSession + "__" + channelId;
    }

}
