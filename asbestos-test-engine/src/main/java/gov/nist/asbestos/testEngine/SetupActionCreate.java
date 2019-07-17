package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupActionCreate {
    private FixtureMgr fixtureMgr;  // static fixtures and history of operations
    private ValE val;
    private FixtureComponent fixtureComponent = null;
    private FhirClient fhirClient = null;
    private VariableMgr variableMgr = null;
    private URI sut = null;
    private String type = null;
    private String resourceType = null;  // used in autoCreate

    SetupActionCreate(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    /**
     * for autocreates
     * @param fixtureId
     * @param reference
     * @param operationReport
     */
    void run(String fixtureId, Reference reference, TestReport.SetupActionOperationComponent operationReport) {
        Reporter reporter = new Reporter(val, operationReport, type, "");
        FixtureComponent sourceFixture = fixtureMgr.get(fixtureId);
        if (sourceFixture == null) {
            reporter.reportError("reference " + reference + " does not exist");
            return;
        }
        BaseResource resourceToSend = sourceFixture.getResourceResource();
        if (resourceToSend == null) {  // should be impossible
            reporter.reportError("reference " + reference + " does not have a response resource to send");
            return;
        }
        Map<String, String> requestHeader = new HashMap<>();
        Ref targetUrl = OperationURLBuilder.build(null, sut, fixtureMgr, reporter, resourceToSend.getClass());
        if (targetUrl == null)
            return;
        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, Format.JSON, requestHeader);
        if (wrapper.isOk())
            reporter.report(wrapper.getRef() + " created");
        else
            reporter.report(wrapper.getRef() + " not created");
        fixtureComponent = new FixtureComponent(fixtureId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase());
        fixtureMgr.put(fixtureId, fixtureComponent);
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
                : OperationURLBuilder.build(op, sut, fixtureMgr, reporter, resourceToSend.getClass());
        if (targetUrl == null)
            return;

        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, format, requestHeader);
        if (wrapper.isOk())
            reporter.report(wrapper.getRef() + " created");
        else
            reporter.report(wrapper.getRef() + " not created");
        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        fixtureComponent = new FixtureComponent(fixtureId)
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

    SetupActionCreate setVal(ValE val) {
        this.val = val;
        return this;
    }

    public FixtureComponent getFixtureComponent() {
        return fixtureComponent;
    }

    public SetupActionCreate setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    private FhirClient getFhirClient() {
        if (fhirClient == null)
            fhirClient = new FhirClient();
        return fhirClient;
    }

    public SetupActionCreate setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    public SetupActionCreate setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public SetupActionCreate setType(String type) {
        this.type = type;
        return this;
    }

    public SetupActionCreate setResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }
}
