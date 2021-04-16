package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.testEngine.engine.ExtensionDef;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetupIT {
    private static String testSession = "default";
    private static String channelName = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;
    private static Ref base500;

    @BeforeAll
    static void createTheChannel() throws IOException, URISyntaxException {
        new HttpDelete().run(String.format("http://localhost:%s/asbestos/channel/%s__%s", ITConfig.getProxyPort(), testSession, channelName));
        base = new URI(Utility.createChannel(testSession, channelName, fhirPort, proxyPort));
        base500 = ChannelsForTests.gen500();
    }

    @Test
    void assertTest() throws URISyntaxException {
        TestEngine engine = Utility.run(base, "/engine/setup/TestScript.xml");

        // no failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // assert in setup failed
        assertEquals(TestReport.TestReportActionResult.FAIL, engine.getTestReport().getSetup().getAction().get(1).getAssert().getResult());

        // test must fail because of setup failure
        assertEquals(TestReport.TestReportResult.FAIL, engine.getTestReport().getResult());

        // test.action must be skipped because of setup failure
        assertEquals(TestReport.TestReportActionResult.SKIP, engine.getTestReport().getTest().get(0).getAction().get(0).getAssert().getResult());

    }

    @Test
    void opTest() throws URISyntaxException {
        TestEngine engine = Utility.run(base500.getUri(), "/engine/setup2/TestScript.xml");

        // no top-level script failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // op in setup failed - no assert after - op must report error
        assertEquals(TestReport.TestReportActionResult.ERROR, engine.getTestReport().getSetup().getAction().get(0).getOperation().getResult());

        // test must fail because of setup failure
        assertEquals(TestReport.TestReportResult.FAIL, engine.getTestReport().getResult());

        // test.action must be skipped because of setup failure
        assertEquals(TestReport.TestReportActionResult.SKIP, engine.getTestReport().getTest().get(0).getAction().get(0).getOperation().getResult());

    }
}
