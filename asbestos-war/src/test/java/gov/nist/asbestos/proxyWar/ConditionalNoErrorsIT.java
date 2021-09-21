package gov.nist.asbestos.proxyWar;

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

class ConditionalNoErrorsIT {
    private static String testSession = "default";
    private static String channelName = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    @BeforeAll
    static void createTheChannel() throws IOException, URISyntaxException {
        new HttpDelete().run(String.format("http://localhost:%s/asbestos/rw/channel/%s__%s", proxyPort, testSession, channelName));
        base = new URI(Utility.createChannel(testSession, channelName, fhirPort, proxyPort));
    }

    @Test
    void conditionalNoErrorsTest() throws URISyntaxException {
        TestEngine engine = Utility.run(base, "/engine/conditionalNoErrors/TestScript.xml");

        // no engine failure
        assertEquals(0, engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure).size());

        // assert (action 0) in test 1 fails
        assertEquals(TestReport.TestReportActionResult.FAIL, engine.getTestReport().getTest().get(1).getAction().get(0).getAssert().getResult());

        // test 2 does not run because of noErrors extension
        assertEquals(TestReport.TestReportActionResult.SKIP, engine.getTestReport().getTest().get(2).getAction().get(0).getAssert().getResult());

        // script fails
        assertEquals(TestReport.TestReportResult.FAIL, engine.getTestReport().getResult());
    }
}
