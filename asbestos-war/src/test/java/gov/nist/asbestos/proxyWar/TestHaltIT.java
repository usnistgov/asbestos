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

class TestHaltIT {
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
    void testHaltTest() throws URISyntaxException {
        TestEngine engine = Utility.run(base, "/engine/testHalt/TestScript.xml");

        // no engine failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // assert (action 1) in test 0 fails
        assertEquals(TestReport.TestReportActionResult.FAIL, engine.getTestReport().getTest().get(0).getAction().get(1).getAssert().getResult());

        // assert (action 2) in test 0 is skipped
        // because fail in test skips rest of actions in test
        assertEquals(TestReport.TestReportActionResult.SKIP, engine.getTestReport().getTest().get(0).getAction().get(2).getAssert().getResult());

        // test 1 runs
        assertEquals(TestReport.TestReportActionResult.PASS, engine.getTestReport().getTest().get(1).getAction().get(0).getAssert().getResult());

        // script fails
        assertEquals(TestReport.TestReportResult.FAIL, engine.getTestReport().getResult());

    }
}
