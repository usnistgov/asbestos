package gov.nist.asbestos.proxyWar.delegation;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.proxyWar.ExternalCache;
import gov.nist.asbestos.proxyWar.ITConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
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
    void simpleCallTest() throws URISyntaxException {
        run("/delegation/callTest/TestScript.xml");
    }

    @Test
    void libraryCallTest() throws URISyntaxException {
        // This reuses module.xml from simpleCallTest
        run("/delegation/libraryTest/TestScript.xml");
    }

    TestEngine run(String testScriptLocation) throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource(testScriptLocation).toURI()).getParent().toFile();

        File patientCacheDir = new EC(ExternalCache.getExternalCache()).getTestLogCacheDir("default__default");
        patientCacheDir.mkdirs();

        ModularEngine modularEngine = new ModularEngine(test1, base).setSaveLogs(true);
        TestEngine mainTestEngine = modularEngine.getMainTestEngine();
        modularEngine
                .setVal(val)
                .setTestSession(testScriptLocation)
                .setChannelId("default__default")
                .setExternalCache(ExternalCache.getExternalCache())
                .setFhirClient(new FhirClient())
                .addCache(patientCacheDir)
                .runTest();
        int i = 0;
        for (TestEngine engine : modularEngine.getTestEngines()) {
            System.out.println("ENGINE " + i);
            System.out.println(engine.getTestReportAsJson());
            i++;
        }
        TestReport report = mainTestEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        return mainTestEngine;
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
