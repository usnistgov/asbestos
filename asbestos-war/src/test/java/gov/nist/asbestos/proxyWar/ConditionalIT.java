package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.testEngine.engine.ExtensionDef;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalIT {
    private static String testSession = "default";
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    @BeforeAll
    static void createTheChannel() throws IOException, URISyntaxException {
        base = new URI(Utility.createChannel(testSession, channelId, fhirPort, proxyPort));
    }

    @BeforeAll
    static void deleteAllInstances() throws URISyntaxException {
        assertNotNull(base);

        FhirClient fhirClient = new FhirClient();
        List<String> params = new ArrayList<>();
        params.add("given=Donald");
        params.add("family=Duck");
        List<ResourceWrapper> wrappers = fhirClient.search(new Ref(base), Patient.class, params, false, false);
        for (ResourceWrapper wrapper : wrappers) {
            Ref ref = wrapper.getRef();
            fhirClient.deleteResource(ref, new HashMap<>());
        }

        wrappers = fhirClient.search(new Ref(base), Patient.class, params, false, false);
        assertTrue(wrappers.isEmpty());
    }

    @Test
    void submissionTest() throws URISyntaxException {

        //
        // First time submission - actual submission happens
        //

        TestEngine engine = Utility.run(base, "/engine/conditional/TestScript.xml");
        assertEquals(TestReport.TestReportResult.PASS, engine.getTestReport().getResult());

        // first time submission should happen
        engine.getTestReport().getTest().get(2).getAction().get(1).getOperation().getResult().equals(TestReport.TestReportActionResult.PASS);

        TestReport.TestReportSetupComponent setups = engine.getTestReport().getSetup();
        assertEquals(0, setups.getAction().size());

        List<Extension> failures = engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure);
        assertEquals(0, failures.size());


        //
        // Second time submission - actual submission does not happen
        //

        TestEngine engine2 = Utility.run(base, "/engine/conditional/TestScript.xml");
        assertEquals(TestReport.TestReportResult.PASS, engine2.getTestReport().getResult());

        // second time should return a skip for the load
        engine2.getTestReport().getTest().get(2).getAction().get(1).getOperation().getResult().equals(TestReport.TestReportActionResult.SKIP);

        TestReport.TestReportSetupComponent setups2 = engine2.getTestReport().getSetup();
        assertEquals(0, setups2.getAction().size());

        List<Extension> failures2 = engine.getTestReport().getExtensionsByUrl(ExtensionDef.failure);
        assertEquals(0, failures2.size());
    }
}
