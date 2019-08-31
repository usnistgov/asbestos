package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

class ReadTest {
    @Test
    void createPatientAndRead() throws URISyntaxException {
        FhirClient fhirClientMock = mock(FhirClient.class);
        ResourceWrapper wrapper = new ResourceWrapper();
        HttpPost poster = new HttpPost();
        poster.setStatus(200);
        wrapper.setHttpBase(poster);
        Patient patient = new Patient().addName(new HumanName().setFamily("Fred"));
        wrapper.setResource(patient);
        String url = "http://localhost:9999/fhir/Patient/45";
        poster.setLocation(url);
        wrapper.setRef(new Ref(url));


        when(fhirClientMock.writeResource(any(BaseResource.class), any(Ref.class), eq(Format.XML), any(Map.class))).thenReturn(wrapper);
        when(fhirClientMock.getFormat()).thenReturn(Format.XML);
        Map<String, String> headers = new HashMap<>();
        headers.put("accept-charset", "utf-8");
        headers.put("accept", "json");
        when(fhirClientMock.readResource(new Ref(url), headers)).thenReturn(wrapper);

        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/variable/createread/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(fhirClientMock)
                .run();
        List<String> errors = testEngine.getErrors();
        printErrors(errors);
        assertEquals(0, errors.size());
        TestReport report = testEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        System.out.println(val.toString());
        assertFalse(val.hasErrors());
    }

    private void printErrors(List<String> errors) {
        if (errors.isEmpty())
            return;
        System.out.println("Errors:\n" + errors);
    }
}
