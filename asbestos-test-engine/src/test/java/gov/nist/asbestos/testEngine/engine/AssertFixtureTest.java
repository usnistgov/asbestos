package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import gov.nist.asbestos.client.client.Format;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssertFixtureTest {

    private static Logger log = Logger.getLogger(AssertFixtureTest.class.getName());

    private TestEngine runTestEngine(File test1) throws URISyntaxException {
        Val val = new Val();
        File externalCache = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI("http://localhost"), null)
                .setTestSession(this.getClass().getSimpleName())
                .setExternalCache(externalCache)
                .setVal(val)
                .runTest();
        return testEngine;
    }

    @Test
    void verifyPatientName() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        log.info(ParserBase.encode(report, Format.JSON));
        assertEquals(TestReport.TestReportResult.PASS, result);
    }


    @Test
    void warningOnlyMissing() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/warningOnlyMissing/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("warningOnly is required but missing", errors.get(0));
    }

    @Test
    void patientNameWarning() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameWarning/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        List<String> warnings = testEngine.getTestReportWarnings();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
        assertEquals(1, warnings.size());
    }

    @Test
    void patientNameError() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameError/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
    }

    @Test
    void patientNameGood() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientNameWithSourceId() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientNameBadWithSourceId() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameBadWithSourceId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = runTestEngine(test1);
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expression Patient.name.family = 'Brown' failed."));
    }
    
    private void printErrors(List<String> errors) {
        if (errors.isEmpty())
            return;
        log.log(Level.SEVERE, "" + errors);
    }
}
