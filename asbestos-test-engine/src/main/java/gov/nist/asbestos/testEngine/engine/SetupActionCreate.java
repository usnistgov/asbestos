package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class SetupActionCreate extends GenericSetupAction {

    SetupActionCreate(ActionReference actionReference, FixtureMgr fixtureMgr, boolean isFollowedByAssert) {
        super(actionReference, isFollowedByAssert);
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    /**
     * for autocreates
     * @param fixtureId
     * @param reference
     * @param operationReport
     */
    void run(TestScript testScript, TestScript.TestScriptFixtureComponent comp, String fixtureId, Reference reference, TestReport.SetupActionOperationComponent operationReport) {
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
        Ref targetUrl = OperationURLBuilder.build(null, sut, fixtureMgr, reporter, resourceToSend.getClass().getSimpleName());
        if (targetUrl == null)
            return;
        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, Format.XML, requestHeader);

        if (wrapper.isOk())
            reporter.report(wrapper.getRef() + " created", wrapper);
        else
            reporter.reportError(wrapper.getRef() + " not created", wrapper);
        ActionReference action = new ActionReference(testScript, comp);
//        UIEvent uiEvent = getUIEvent(wrapper);
        UIEvent uiEvent = new UIEvent(new EC(getTestEngine().getExternalCache())).fromResource(wrapper);
        //fixtureMgr.add(fixtureId)
        sourceFixture
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase())
                .setCreatedByActionReference(action)
                .setCreatedByUIEvent(uiEvent);
        if (wrapper.isOk())
            reporter.report(wrapper.getRef() + " created", wrapper);

    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        if (!preExecute(op, operationReport))
            return;

        if (resourceToSend == null) {
            reporter.reportError("sourceId " + op.getSourceId() + " does not have a response resource to send");
            return;
        }

        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, format, requestHeader);

        postExecute(wrapper, operationReport, isFollowedByAssert);
    }


    String resourceTypeToSend() {
        return resourceToSend.getClass().getSimpleName();
    }

    @Override
    Ref buildTargetUrl() {
        return op.hasUrl()
                ? new Ref(op.getUrl())
                : OperationURLBuilder.build(op, sut, fixtureMgr, reporter, resourceTypeToSend());
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
        super.setVariableMgr(variableMgr);
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
