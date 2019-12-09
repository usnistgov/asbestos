package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimumIdTest {

    @Test
    void wrongType() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/wrongType/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("minimumId: cannot compare org.hl7.fhir.r4.model.Patient and org.hl7.fhir.r4.model.DocumentReference", errors.get(0));
    }

    @Test
    void missingSubject() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/missingSubject/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("minimumId: attribute Subject not found", errors.get(0));

    }

    @Test
    void hasExtra() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/hasExtra/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }
}
