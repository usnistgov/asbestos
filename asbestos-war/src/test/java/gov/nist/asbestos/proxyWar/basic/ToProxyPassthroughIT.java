package gov.nist.asbestos.proxyWar.basic;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.proxyWar.support.ITConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
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
    private static String channelId = "fhirpass";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();
    private static URI base;

    /**
     * create patient and verify the returned reference is via the proxy
     * @throws URISyntaxException
     */
    @Test
    void createPatient() throws URISyntaxException {
        TestEngine testEngine = run("/toProxy/createPatient/TestScript.xml");
        TestReport testReport = testEngine.getTestReport();
        System.out.println("foo");
        String message = testReport.getTest().get(0).getAction().get(0).getOperation().getMessage();
        int httpI = message.indexOf("http");
        assertNotEquals(-1, httpI);
        String http  = message.substring(httpI); // has extra at the end
        assertTrue(http.contains("prox"));
    }

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
     static void beforeAll() throws IOException, URISyntaxException {
        base = new URI(createChannel());
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
