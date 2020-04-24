package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
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
    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;

    abstract String resourceTypeToSend();

//    private String asMarkdown(Map<String, String> table, String title) {
//        StringBuilder buf = new StringBuilder();
//        buf.append("### ").append(title).append("\n");
//        boolean first = true;
//        for (String key : table.keySet()) {
//            if (!first)
//                buf.append("\n");
//            first = false;
//            String value = table.get(key);
//            buf.append("**").append(key).append("**: ").append(value);
//        }
//        return buf.toString();
//    }

    private void reportOperation(ResourceWrapper wrapper) {
        Objects.requireNonNull(testCollectionId);
        Objects.requireNonNull(testId);
        Objects.requireNonNull(testEngine);
        new ActionReporter()
                .setTestEngine(testEngine)
                .setTestCollectionId(testCollectionId)
                .setTestId(testId)
                .reportOperation(wrapper, fixtureMgr, variableMgr, reporter);
//        String request = "### " + wrapper.getHttpBase().getVerb() + " " + wrapper.getHttpBase().getUri() + "\n";
//
//        Map<String, String> fixtures = new HashMap<>();
//        for (String key : fixtureMgr.keySet()) {
//            FixtureComponent comp = fixtureMgr.get(key);
//            String value = null;
//
//            HttpBase httpBase = comp.getHttpBase();
//            if (httpBase != null) {
//                Headers responseHeaders = httpBase.getResponseHeaders();
//                String eventUrl = responseHeaders.getProxyEvent();
//                value = EventLinkToUILink.get(eventUrl);
//            } else if (comp.isLoaded()){
//                ResourceWrapper wrapper1 = comp.getResourceWrapper();
//                if (wrapper1 != null) {
//                    Ref ref = wrapper1.getRef();
//                    if (ref != null)
//                        value = ref.toString() + " (static)";
//                }
//            }
//
//            try {
//                Ref ref = new Ref(value);
//                if (ref.isAbsolute()) {
//                    value = "<a href=\"" + value + "\"" + " target=\"_blank\">" + value + "</a>";
//                }
//            } catch (Throwable t) {
//                // ignore
//            }
//
//            fixtures.put(key, value);
//        }
//
//        Map<String, String> variables = variableMgr.getVariables();
//
//        String markdown = request
//                + ActionReporter.asMarkdown(fixtures, "Fixtures")
//                + "\n"
//                + ActionReporter.asMarkdown(variables, "Variables");
//
//        reporter.report(markdown, wrapper);
    }

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
            FixtureComponent sourceFixture = fixtureMgr.get(op.getSourceId());
            if (sourceFixture == null) {
                reporter.reportError("sourceId " + op.getSourceId() + " does not exist");
                return false;
            }
            resourceToSend = sourceFixture.getResourceResource();
            resourceToSend = updateResourceToSend(resourceToSend);
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

    public BaseResource updateResourceToSend(BaseResource resource) {
        String resourceString = ProxyBase.encode(resource, Format.JSON);
        String updatedResourceString = variableMgr.updateReference(resourceString);
        return ProxyBase.parse(updatedResourceString, Format.JSON);
    }

    void postExecute(ResourceWrapper wrapper) {
        Objects.requireNonNull(testEngine);

        if (wrapper.hasResource()) {
            String receivedResourceType = wrapper.getResource().getClass().getSimpleName();
            String expectedResourceType = resourceTypeToBeReturned();
            if (expectedResourceType != null && !receivedResourceType.equals(expectedResourceType)) {
                reporter.reportError("Expected resource of type " +  expectedResourceType + " received " + receivedResourceType + " instead");
                return;
            }
        }

        String fixtureId = op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
//        fixtureComponent = new FixtureComponent(fixtureId)
        fixtureMgr.add(fixtureId)
//                .setTestCollectionId(testCollectionId)
//                .setTestId(testId)
                .setResource(wrapper)
                .setHttpBase(wrapper.getHttpBase());
        //fixtureMgr.put(fixtureId, fixtureComponent);

        reportOperation(wrapper);
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

    public GenericSetupAction setTestCollectionId(String testCollectionId) {
        this.testCollectionId = testCollectionId;
        return this;
    }

    public GenericSetupAction setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public GenericSetupAction setTestEngine(TestEngine testEngine) {
        this.testEngine = testEngine;
        return this;
    }
}
