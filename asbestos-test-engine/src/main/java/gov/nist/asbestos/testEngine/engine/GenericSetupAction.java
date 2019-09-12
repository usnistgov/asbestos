package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.*;

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
    TestReport.SetupActionOperationComponent opReport;
    TestReport testReport = null;
    URI base = null;
    Ref targetUrl;
    Format format;
    Map<String, String> requestHeader = new HashMap<>();
    BaseResource resourceToSend;
    Reporter reporter;
    String label;

    abstract String resourceTypeToSend();
    abstract Ref buildTargetUrl();

    static void handleRequestHeader(Map<String, String> requestHeader, TestScript.SetupActionOperationComponent op, VariableMgr variableMgr) {
        List<TestScript.SetupActionOperationRequestHeaderComponent> hdrs = op.getRequestHeader();
        for (TestScript.SetupActionOperationRequestHeaderComponent hdr : hdrs) {
            String value = hdr.getValue();
            value = variableMgr.updateReference(value);
            requestHeader.put(hdr.getField(), value);
        }
    }

    boolean preExecute(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(op);
        Objects.requireNonNull(operationReport);
        Objects.requireNonNull(variableMgr);
        val = new ValE(val).setMsg(type);
        this.op = op;
        this.opReport = operationReport;

        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        label = op.hasLabel() ? op.getLabel() : "No Label";
        reporter = new Reporter(val, operationReport, type, label);
        format = op.hasContentType() && op.getContentType().contains("json") ? Format.JSON : Format.XML;
        if (isPUTorPOST()) {
            if (!op.hasSourceId()) {
                reporter.reportError("has no sourceId on operation " + op.getType().getCode());
                return false;
            }
            FixtureComponent sourceFixture = fixtureMgr.get(op.getSourceId());
            if (sourceFixture == null) {
                reporter.reportError("sourceId " + op.getSourceId() + " does not exist");
                return false;
            }
            resourceToSend = sourceFixture.getResourceResource();
            updateResourceToSend(resourceToSend);
        }
        if (op.hasRequestHeader())
            handleRequestHeader(requestHeader, op, variableMgr);
        if (!requestHeader.containsKey("accept-charset"))
            requestHeader.put("accept-charset", "utf-8");
        if (op.hasAccept())
            requestHeader.put("accept", Format.fromContentType(op.getAccept()).getContentType());


        targetUrl = buildTargetUrl();
        if (targetUrl == null) {
            reporter.reportError("cannot generate targetUrl");
        }
        return targetUrl != null;
    }

    public void updateResourceToSend(BaseResource resource) {

    }

    void postExecute(ResourceWrapper wrapper) {
        if (wrapper.hasResource()) {
            String receivedResourceType = wrapper.getResource().getClass().getSimpleName();
            String expectedResourceType = resourceTypeToBeReturned();
            if (expectedResourceType != null && !receivedResourceType.equals(expectedResourceType)) {
                reporter.reportError("Expected resource of type " +  expectedResourceType + " received " + receivedResourceType + " instead");
                return;
            }
        }

        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        fixtureComponent = new FixtureComponent(fixtureId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase());
        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    String resourceTypeToBeReturned() {
        return resourceTypeToSend();
    }

    private List<String> putPostTypes = new ArrayList<String>(
            Arrays.asList(
            "update", "updateCreate", "create", "transaction", "mhd-pdb-transaction"
            )
    );

    boolean isPUTorPOST() {
        String type = op.getType().getCode();
        return putPostTypes.contains(type);
    }
}
