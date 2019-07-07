package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssertFixtureTest {

    @Test
    void verifyPatientName() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);

        // verify test name
        TestScript testScript = testEngine.getTestScript();
        assertEquals(testScript.getName(), report.getName());
    }

    @Test
    void warningOnlyMissing() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/warningOnlyMissing/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("setup.action.assert : No Label : warningOnly is required but missing", errors.get(0));
    }

    @Test
    void patientNameWarning() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameWarning/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientNameError() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameError/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
    }

    @Test
    void patientNameGood() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientNameWithSourceId() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientNameBadWithSourceId() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameBadWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Assertion failed"));
    }



    private void printErrors(List<String> errors) {
        if (errors.isEmpty())
            return;
        System.out.println("Errors:\n" + errors);
    }
}
