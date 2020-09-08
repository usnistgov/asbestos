package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SetupActionTransaction extends GenericSetupAction {

    SetupActionTransaction(ActionReference actionReference, FixtureMgr fixtureMgr, boolean isFollowedByAssert) {
        super(actionReference,isFollowedByAssert);
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    ResourceWrapper run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        if (!preExecute(op, operationReport))
            return null;

        ResourceWrapper wrapper = getFhirClient().writeResource(resourceToSend, targetUrl, fhirClient.getFormat(), requestHeader);

        postExecute(wrapper, operationReport, isFollowedByAssert);
        return wrapper;
    }

    @Override
    Ref buildTargetUrl() {
        return op.hasUrl()
                ? new Ref(op.getUrl())
                : OperationURLBuilder.build(op, sut, fixtureMgr, reporter, resourceTypeToSend());
    }

    String resourceTypeToSend() {
        return null;
    }

    private String getBundleIssues(Bundle bundle) {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            Bundle.BundleEntryResponseComponent responseComponent = component.getResponse();
            Resource outcome = responseComponent.getOutcome();
            if (outcome instanceof OperationOutcome) {
                OperationOutcome oo = (OperationOutcome) outcome;
                if (!first)
                    buf.append("\n");
                first = false;
                boolean firstIssue = true;
                for (String issue : getOperationOutcomeIssues(oo)) {
                    if (!firstIssue)
                        buf.append("\n");
                    buf.append(issue);
                    firstIssue = false;
                }
//                buf.append(getOperationOutcomeIssues(oo));
            }
        }

        return buf.toString();
    }

    private List<String> getOperationOutcomeIssues(OperationOutcome oo) {
        List<String> issues = new ArrayList<>();

        for (OperationOutcome.OperationOutcomeIssueComponent issueComponent : oo.getIssue()) {
            if (issueComponent.getSeverity() == OperationOutcome.IssueSeverity.ERROR ) {
                String details = issueComponent.getDiagnostics();
                issues.add(details);
            }
        }

        return issues;
    }

    private boolean bundleContainsError(Bundle bundle) {
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                Bundle.BundleEntryResponseComponent response = component.getResponse();
                if (response.hasStatus() && !response.getStatus().startsWith("20")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean operationOutcomeContainsError(OperationOutcome oo) {
        for (OperationOutcome.OperationOutcomeIssueComponent component : oo.getIssue()) {
            if (component.getSeverity() == OperationOutcome.IssueSeverity.ERROR || component.getSeverity() == OperationOutcome.IssueSeverity.FATAL) {
                return true;
            }
        }
        return false;
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
        super.setVariableMgr(variableMgr);
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
