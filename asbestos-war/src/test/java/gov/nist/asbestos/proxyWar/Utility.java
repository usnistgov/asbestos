package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

public class Utility {

    static String createChannel(String testSession, String channelId, String fhirPort, String proxyPort) throws URISyntaxException, IOException {
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

    static TestEngine run(URI serverBase, String testScriptLocation) throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(ConditionalIT.class.getResource(testScriptLocation).toURI()).getParent().toFile();

        ModularEngine modularEngine = new ModularEngine(test1, serverBase).setSaveLogs(true);
        TestEngine mainTestEngine = modularEngine.getMainTestEngine();
        modularEngine
                .setVal(val)
                .setTestSession(testScriptLocation)
                .setChannelId("default__default")
                .setExternalCache(ExternalCache.getExternalCache())
                .setFhirClient(new FhirClient())
                .runTest();
        int i = 0;
        for (TestEngine engine : modularEngine.getTestEngines()) {
            System.out.println("ENGINE " + i);
            System.out.println(engine.getTestReportAsJson());
            i++;
        }
        TestReport report = mainTestEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        //assertEquals(TestReport.TestReportResult.PASS, result);
        return mainTestEngine;
    }
}
