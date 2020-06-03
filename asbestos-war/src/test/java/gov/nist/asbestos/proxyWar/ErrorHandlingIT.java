package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.testEngine.engine.ExtensionDef;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorHandlingIT {
    private static String testSession = "default";
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static Ref base500;

    @BeforeAll
    static void createTheChannel() throws IOException, URISyntaxException {
        base500 = ChannelsForTests.gen500();
    }

    // has assert - op fails => depends on assert to judge
    @Test
    void withAssert() throws URISyntaxException {
        TestEngine engine = Utility.run(base500.getUri(), "/engine/errorHandling/withAssert/TestScript.xml");

        // no top-level script failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // GET fails but op succeeds because of following assert
        assertEquals(TestReport.TestReportActionResult.PASS, engine.getTestReport().getTest().get(0).getAction().get(0).getOperation().getResult());

        // overall script Passes - assert exceped GET to fail
        assertEquals(TestReport.TestReportResult.PASS, engine.getTestReport().getResult());
    }

    // no assert - non-2xx status means failure
    @Test
    void withoutAssert() throws URISyntaxException {
        TestEngine engine = Utility.run(base500.getUri(), "/engine/errorHandling/withoutAssert/TestScript.xml");

        // no top-level script failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // GET fails and op not followed by assert so op fails
        assertEquals(TestReport.TestReportActionResult.ERROR, engine.getTestReport().getTest().get(0).getAction().get(0).getOperation().getResult());

        // overall script fails -
        assertEquals(TestReport.TestReportResult.FAIL, engine.getTestReport().getResult());

    }

}
