package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

abstract class GenericSetupAction {
    private static Logger log = Logger.getLogger(GenericSetupAction.class.getName());
    FixtureMgr fixtureMgr;  // static fixtures and history of operations
    ValE val;
    FixtureComponent fixtureComponent = null;
    FixtureComponent sourceFixture = null;
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
    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;
    ActionReference actionReference = null;
    String channelId = null;
    boolean isFollowedByAssert;

    abstract String resourceTypeToSend();

    GenericSetupAction(ActionReference actionReference, boolean isFollowedByAssert) {
        this.actionReference = actionReference;
        this.isFollowedByAssert = isFollowedByAssert;
        Objects.requireNonNull(actionReference);
    }

    private void reportOperation(ResourceWrapper wrapper) {
        Objects.requireNonNull(testCollectionId);
        Objects.requireNonNull(testId);
        Objects.requireNonNull(testEngine);

        testEngine.reportOperation(wrapper, reporter, op);
//        new ActionReporter()
//                .setTestEngine(testEngine)
//                .setTestCollectionId(testCollectionId)
//                .setTestId(testId)
//                .reportOperation(wrapper, fixtureMgr, variableMgr, reporter, op);
    }

    abstract Ref buildTargetUrl();

    static void updateRequestHeader(Map<String, String> requestHeader, TestScript.SetupActionOperationComponent op, VariableMgr variableMgr) {
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
        Objects.requireNonNull(testEngine);

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
            sourceFixture = fixtureMgr.get(op.getSourceId());
            if (sourceFixture == null) {
                reporter.reportError("sourceId " + op.getSourceId() + " does not exist");
                return false;
            }
            sourceFixture.setReferencedByActionReference(actionReference);
            resourceToSend = sourceFixture.getResourceResource();
            if (resourceToSend == null) {
                reporter.reportError("sourceId " + sourceFixture.getId() + "  does not reference a resource");
                return false;
            }
            resourceToSend = updateResourceToSend(sourceFixture);
        }
        if (op.hasRequestHeader())
            updateRequestHeader(requestHeader, op, variableMgr);
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

    public BaseResource updateResourceToSend(FixtureComponent toSend) {
        ResourceWrapper wrapper = toSend.getResourceWrapper();
        if (wrapper == null) {
            reporter.reportError("Update to Fixture " + toSend.getId() + " - contains no resource");
            return null;
        }
        BaseResource resource = wrapper.getResource();
        String resourceString = ParserBase.encode(resource, Format.JSON);
        String updatedResourceString = variableMgr.updateReference(resourceString);
        if (updatedResourceString == null) {
            reporter.reportError("Update to Fixture " + toSend.getId() + " failed");
            return resource;
        }
        resource = ParserBase.parse(updatedResourceString, Format.JSON);
        wrapper.setResource(resource);
        return resource;
    }

    UIEvent getUIEvent(ResourceWrapper wrapper) {
        Objects.requireNonNull(getTestEngine());
        EC ec = new EC(getTestEngine().getExternalCache());
        UIEvent sameChannelUIEvent = ec.getEvent(getTestEngine().getTestSession(),
                        getTestEngine().getChannelId(),
                        wrapper.getResourceType(),
                        wrapper.getEventId());
        UIEvent targetUriBasedEvent = new UIEvent(getTestEngine().getEC()).fromURI(targetUrl.getUri());
        if (targetUriBasedEvent != null) {
            if (!sameChannelUIEvent.getTestSession().equals(targetUriBasedEvent.getTestSession())
                    || !sameChannelUIEvent.getChannelId().equals(targetUriBasedEvent.getChannelId())) {
                return resetProxyChannel(sameChannelUIEvent, targetUriBasedEvent.getTestSession(), targetUriBasedEvent.getChannelId());
            }
        }
        return sameChannelUIEvent;
    }

    private UIEvent resetProxyChannel(UIEvent uiEvent, String testSession, String channelId) {
        uiEvent.setTestSession(testSession);
        uiEvent.setChannelId(channelId);
        return uiEvent;
    }

    void postExecute(ResourceWrapper wrapper, TestReport.SetupActionOperationComponent operationReport, boolean isFollowedByAssert) {
        Objects.requireNonNull(testEngine);

        if (!isFollowedByAssert) {
            if (wrapper.isOk()) {
                reporter.report("CREATE " + wrapper.getRef(), wrapper);
            } else {
                reporter.report("create to " + targetUrl + " failed with status " + wrapper.getHttpBase().getStatus(), wrapper);
                operationReport.setResult(TestReport.TestReportActionResult.FAIL);
            }

            if (wrapper.hasResource()) {
                String receivedResourceType = wrapper.getResource().fhirType();
                String expectedResourceType = resourceTypeToBeReturned();
                if (expectedResourceType != null && !receivedResourceType.equals(expectedResourceType)) {
                    reporter.reportError("Expected resource of type " + expectedResourceType + " received " + receivedResourceType + " instead");
                    //return;
                }
            }
        }

        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        UIEvent uiEvent = getUIEvent(wrapper);
        fixtureMgr.add(fixtureId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase())
        .setCreatedByActionReference(actionReference)
        .setCreatedByUIEvent(uiEvent);

        if (sourceFixture != null) {
            sourceFixture.setCreatedByUIEvent(uiEvent);
        }

        testEngine.updateParentFixtureOut(op);
        reportOperation(wrapper);
    }

    String resourceTypeToBeReturned() {
        return resourceTypeToSend();
    }

    private final List<String> putPostTypes = new ArrayList<String>(
            Arrays.asList(
            "update", "updateCreate", "create", "transaction", "mhd-pdb-transaction", "ftkValidate"
            )
    );

    boolean isPUTorPOST() {
        String type = op.getType().getCode();
        return putPostTypes.contains(type);
    }

    public GenericSetupAction setTestCollectionId(String testCollectionId) {
        this.testCollectionId = testCollectionId;
        return this;
    }

    public GenericSetupAction setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public TestEngine getTestEngine() {
        return testEngine;
    }

    public GenericSetupAction setTestEngine(TestEngine testEngine) {
        this.testEngine = testEngine;
        return this;
    }

    public String getChannelId() {
        return channelId;
    }

    public GenericSetupAction setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public GenericSetupAction setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }
}
