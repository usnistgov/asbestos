package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendStaticFixtureTest {

    @Test
    void verifyPatientName() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .run();
        TestReport report = testEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
    }

    @Test
    void warningOnlyMissing() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/warningOnlyMissing/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("setup.action.assert : No ID : warningOnly is required but missing", errors.get(0));
    }

    @Test
    void patientNameWarning() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameWarning/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
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
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameError/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        //assertEquals("setup.action.assert : No ID : warningOnly is required but missing", errors.get(0));
    }

    @Test
    void patientNameGood() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
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
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
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
        File test1 = Paths.get(getClass().getResource("/setup/SendStaticFixture/patientNameBadWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .run();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(0, errors.size());
    }



    private void printErrors(List<String> errors) {
        if (errors.isEmpty())
            return;
        System.out.println("Errors:\n" + errors);
    }
}
