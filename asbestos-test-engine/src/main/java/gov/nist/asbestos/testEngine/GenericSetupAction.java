package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.List;
import java.util.Map;

abstract class GenericSetupAction {
    FixtureMgr fixtureMgr;  // static fixtures and history of operations
    ValE val;
    FixtureComponent fixtureComponent = null;
    FhirClient fhirClient = null;
    VariableMgr variableMgr = null;
    URI sut = null;
    String type = null;
    String resourceType = null;  // used in autoCreate
    TestScript.SetupActionOperationComponent op;
    TestReport testReport = null;
    URI base = null;

    static void handleRequestHeader(Map<String, String> requestHeader, TestScript.SetupActionOperationComponent op, VariableMgr variableMgr) {
        List<TestScript.SetupActionOperationRequestHeaderComponent> hdrs = op.getRequestHeader();
        for (TestScript.SetupActionOperationRequestHeaderComponent hdr : hdrs) {
            String value = hdr.getValue();
            value = variableMgr.updateReference(value);
            requestHeader.put(hdr.getField(), value);
        }
    }

}
