package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MhdToFhirServerIT {
    private static FhirContext ctx;
    private IGenericClient client;
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        ctx = FhirContext.forR4();
        fhirPort = ITConfig.getFhirPort();
        proxyPort = ITConfig.getProxyPort();
    }

    @Test
    void patientToFhir() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/mhdToFhirServer/createPatient/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(ITConfig.getFhirBase()))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        System.out.println(testEngine.getTestReportAsJson());
        TestReport report = testEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);

    }
}
