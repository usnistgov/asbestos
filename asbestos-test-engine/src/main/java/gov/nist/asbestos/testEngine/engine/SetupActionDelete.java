package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupActionDelete extends GenericSetupAction {

    SetupActionDelete(ActionReference actionReference, FixtureMgr fixtureMgr, boolean isFollowedByAssert) {
        super(actionReference, isFollowedByAssert);
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    /**
     * for autodeletes
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
        Map<String, String> requestHeader = new HashMap<>();
        Ref createUrl = sourceFixture.getResourceWrapper().getRef();  // contains version (_history/1) at end
        if (createUrl == null || createUrl.toString().equals(""))
            return;
        Ref targetUrl = new Ref(createUrl.getBase(), createUrl.getResourceType(), createUrl.getId(), null);
        ResourceWrapper wrapper = getFhirClient().deleteResource(targetUrl, requestHeader);

        //reportOperation(wrapper);

        if (wrapper.isOk())
            reporter.report(targetUrl + " deleted", wrapper);
        else {
            reporter.reportFail(targetUrl + " not deleted", wrapper);
        }
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(operationReport);
        Objects.requireNonNull(variableMgr);
        val = new ValE(val).setMsg("setup.create");

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

        Ref targetUrl = sut == null ? null : new Ref(sut);
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
        if (wrapper.isOk())
            reporter.report(wrapper.getRef() + " deleted", wrapper);
        else {
            reporter.reportFail(wrapper.getRef() + " not deleted", wrapper);
        }
//        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
//        fixtureComponent =  new FixtureComponent(fixtureId)
//                .setResource(servlet)
//                .setHttpBase(servlet.getHttpBase());
//        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    @Override
    String resourceTypeToSend() {
        return null;
    }

    @Override
    Ref buildTargetUrl() {
        return null;
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

    SetupActionDelete setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public SetupActionDelete setType(String type) {
        this.type = type;
        return this;
    }
}
