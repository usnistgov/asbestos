package gov.nist.asbestos.proxyWar.delegation;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.proxyWar.ExternalCache;
import gov.nist.asbestos.proxyWar.ITConfig;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ModuleIT {
    private static String testSession = "default";
    private static String channelId = "fhirpass";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    @Test
    void runScript() throws URISyntaxException {
        run("/delegation/callTest/TestScript.xml");
    }


    TestEngine run(String testScriptLocation) throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource(testScriptLocation).toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, base)
                .setVal(val)
                .setTestSession(testScriptLocation)
                .setExternalCache(ExternalCache.getExternalCache())
                .setFhirClient(new FhirClient())
                .runTest();
        System.out.println(testEngine.getTestReportAsJson());
        TestReport report = testEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        return testEngine;
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
                .setChannelType("fhir")
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
