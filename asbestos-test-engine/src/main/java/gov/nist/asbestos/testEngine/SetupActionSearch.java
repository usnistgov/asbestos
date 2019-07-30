package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.client.resolver.SearchParms;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.UnsupportedEncodingException;
import java.net.URI;

class SetupActionSearch extends SetupActionRead {


    SetupActionSearch(FixtureMgr fixtureMgr) {
        super(fixtureMgr);
    }

    @Override
    boolean isSearchOk() {
        return true;
    }

    @Override
    String resourceTypeToSend() {
        if (op.hasResource()) {
            return op.getResource();
        }
        if (op.hasTargetId())
            return new Ref(op.getTargetId()).getResourceType();
        return null;
    }

    @Override
    String resourceTypeToBeReturned() {
        return "Bundle";
    }

    SetupActionSearch setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupActionSearch setBase(URI base) {
        this.base = base;
        return this;
    }

    public SetupActionSearch setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }

    public SetupActionSearch setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    public SetupActionSearch setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public SetupActionSearch setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public SetupActionSearch setType(String type) {
        this.type = type;
        return this;
    }


}
