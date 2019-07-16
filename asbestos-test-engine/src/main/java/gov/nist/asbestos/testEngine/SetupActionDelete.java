package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupActionDelete {
    private FixtureMgr fixtureMgr;  // static fixtures and history of operations
    private ValE val;
    private URI base;
    private FixtureComponent fixtureComponent = null;
    private FhirClient fhirClient = null;
    private VariableMgr variableMgr = null;

    SetupActionDelete(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(operationReport);
        Objects.requireNonNull(variableMgr);
        val = new ValE(val).setMsg("setup.create");

        String type = "teardown.action.operation";
        String label = op.hasLabel() ? op.getLabel() : "No Label";
        Reporter reporter = new Reporter(val, operationReport, type, label);

        if (op.hasTargetId() && op.hasUrl()) {
            reporter.reportError("both targetId and url specified");
            return;
        }
        if (!op.hasTargetId() && !op.hasUrl()) {
            reporter.reportError("targetId or url must be specified");
            return;
        }

        Ref targetUrl = null;
        if (op.hasTargetId()) {
            FixtureComponent targetFixture = fixtureMgr.get(op.getTargetId());
            if (targetFixture == null) {
                reporter.reportError("targetId " + op.getTargetId() + "does not exist");
                return;
            }
            if (targetFixture.hasHttpBase()) {
                HttpBase base = targetFixture.getHttpBase();
                URI uri = base.getUri();
                targetUrl = new Ref(uri);
            } else {
                reporter.reportError("targetId " + op.getTargetId() + "has not been run");
                return;
            }
        }
        if (op.hasUrl()) {
            targetUrl = new Ref(op.getUrl());
        }
        if (targetUrl == null) {
            reporter.reportError("target URL not available");
            return;
        }

        Map<String, String> requestHeader = new HashMap<>();
        if (op.hasRequestHeader()) {
            handleRequestHeader(requestHeader, op, variableMgr);
        }

        ResourceWrapper wrapper = getFhirClient().deleteResource(targetUrl, requestHeader);
        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        fixtureComponent =  new FixtureComponent(fixtureId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase());
        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    static void handleRequestHeader(Map<String, String> requestHeader, TestScript.SetupActionOperationComponent op, VariableMgr variableMgr) {
        List<TestScript.SetupActionOperationRequestHeaderComponent> hdrs = op.getRequestHeader();
        for (TestScript.SetupActionOperationRequestHeaderComponent hdr : hdrs) {
            String value = hdr.getValue();
            value = variableMgr.updateReference(value);
            requestHeader.put(hdr.getField(), value);
        }
    }

    SetupActionDelete setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupActionDelete setBase(URI base) {
        this.base = base;
        return this;
    }

    public FixtureComponent getFixtureComponent() {
        return fixtureComponent;
    }

    SetupActionDelete setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    private FhirClient getFhirClient() {
        if (fhirClient == null)
            fhirClient = new FhirClient();
        return fhirClient;
    }

    SetupActionDelete setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

}
