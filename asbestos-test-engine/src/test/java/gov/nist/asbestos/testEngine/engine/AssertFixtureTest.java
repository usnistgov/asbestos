package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.simapi.validation.Val;

import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nist.asbestos.client.client.Format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssertFixtureTest {

    private static Logger log = Logger.getLogger(AssertFixtureTest.class.getName());

    private TestEngine getTestEngine(File test1) throws URISyntaxException {
        Val val = new Val();
        File externalCache = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI("http://localhost"), null)
                .setTestSession(this.getClass().getSimpleName())
                .setExternalCache(externalCache)
                .setVal(val);
        return testEngine;
    }

    @Test
    void verifyPatientName() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientNameGood/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
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
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expression Patient.name.family = 'Brown' failed."));
    }
    
    @Test
    void patientValidate() throws URISyntaxException, FHIRFormatError, IOException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientValidate/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = getTestEngine(test1);
        ValidationClient validationClient = mock(ValidationClient.class);
        String response = " { \"resourceType\": \"OperationOutcome\",  \"id\": \"cc25118e-e958-4a6c-a179-bc022cd46b78\", \"issue\": [ { \"severity\": \"information\", \"code\": \"informational\", \"diagnostics\": \"No fatal or error issues detected, the validation has passed\" } ] }";
        OperationOutcome  oc = (OperationOutcome) new org.hl7.fhir.r4.formats.JsonParser().parse(response);
        when(validationClient.validate(any(String.class), any(String.class))).thenReturn(oc);
        testEngine.setValidationClient(validationClient);
        testEngine.runTest();
        TestReport report = testEngine.getTestReport();
        String reportXml =  new org.hl7.fhir.r4.formats.XmlParser().composeString(report);
        log.info(reportXml);

        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }

    @Test
    void patientValidateWrongProfileId() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/setup/assertFixture/patientValidateWrongProfileId/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = getTestEngine(test1);
        testEngine.runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
    }

    private void printErrors(List<String> errors) {
        if (errors.isEmpty())
            return;
        log.log(Level.SEVERE, "" + errors);
    }
}
