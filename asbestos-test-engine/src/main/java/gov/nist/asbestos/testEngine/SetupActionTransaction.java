package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SetupActionTransaction extends GenericSetupAction {

    public SetupActionTransaction(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(operationReport);
        Objects.requireNonNull(variableMgr);
        val = new ValE(val).setMsg(type);

        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        String label = op.hasLabel() ? op.getLabel() : "No Label";
        Reporter reporter = new Reporter(val, operationReport, type, label);
        Format format = op.hasContentType() && op.getContentType().contains("json") ? Format.JSON : Format.XML;
        if (!op.hasSourceId()) {
            reporter.reportError("has no sourceId");
            return;
        }
        FixtureComponent sourceFixture = fixtureMgr.get(op.getSourceId());
        if (sourceFixture == null) {
            reporter.reportError("sourceId " + op.getSourceId() + "does not exist");
            return;
        }
        BaseResource resourceToSend = sourceFixture.getResourceResource();
        if (resourceToSend == null) {
            reporter.reportError("sourceId " + op.getSourceId() + " does not have a response resource to send");
            return;
        }

        Map<String, String> requestHeader = new HashMap<>();
        if (op.hasRequestHeader()) {
            handleRequestHeader(requestHeader, op, variableMgr);
        }

        Ref targetUrl = op.hasUrl()
                ? new Ref(op.getUrl())
                : OperationURLBuilder.build(op, sut, fixtureMgr, reporter, null);
        if (targetUrl == null)
            return;

        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, format, requestHeader);
        if (wrapper.isOk()) {
            if (wrapper.getResource() != null && (wrapper.getResource() instanceof Bundle)) {
                //Bundle bundle =
            }
            reporter.report(wrapper.getRef() + " transaction");
        } else {
            reporter.report("transaction to " + targetUrl + " failed with status " + wrapper.getHttpBase().getStatus());
            operationReport.setResult(TestReport.TestReportActionResult.FAIL);
        }
        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        fixtureComponent = new FixtureComponent(fixtureId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase());
        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    public SetupActionTransaction setFixtureMgr(FixtureMgr fixtureMgr) {
        this.fixtureMgr = fixtureMgr;
        return this;
    }

    public SetupActionTransaction setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupActionTransaction setFixtureComponent(FixtureComponent fixtureComponent) {
        this.fixtureComponent = fixtureComponent;
        return this;
    }

    public SetupActionTransaction setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public FhirClient getFhirClient() {
        return fhirClient;
    }

    public SetupActionTransaction setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    public SetupActionTransaction setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public SetupActionTransaction setType(String type) {
        this.type = type;
        return this;
    }

    public SetupActionTransaction setResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

}
