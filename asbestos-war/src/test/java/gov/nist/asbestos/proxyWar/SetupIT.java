package gov.nist.asbestos.proxyWar;

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
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    @BeforeAll
    static void createTheChannel() throws IOException, URISyntaxException {
        base = new URI(Utility.createChannel(testSession, channelId, fhirPort, proxyPort));
    }

    @Test
    void setupTest() throws URISyntaxException {
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
}
