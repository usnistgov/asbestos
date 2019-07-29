package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class SetupActionSearch extends GenericSetupAction{
    private FixtureMgr fixtureMgr;  // static fixtures and history of operations
    private ValE val;
    private URI base;
    private VariableMgr variableMgr = null;
    private FhirClient fhirClient = null;
    private URI sut = null;
    private String type = null;


    SetupActionSearch(FixtureMgr fixtureMgr) {
        this.fixtureMgr = fixtureMgr;
        this.op = op;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent opReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(variableMgr);
        Objects.requireNonNull(testReport);
        Objects.requireNonNull(fhirClient);
        val = new ValE(val).setMsg(type);

        opReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        String label = null;
        Reporter reporter = new Reporter(val, opReport, type, label);


        boolean encodeRequestUrl;
        Map<String, String> requestHeader = new HashMap<>();
        String sourceId = null;
        String targetId = null;  // responseId of a GET or sourceId of POST/PUT
        String url = null;
        Ref ref = (sut == null) ? null : new Ref(sut);

        if (op.hasLabel())
            label = op.getLabel();
        if (op.hasEncodeRequestUrl())
            encodeRequestUrl = op.getEncodeRequestUrl();
        if (op.hasRequestHeader()) {
            SetupActionCreate.handleRequestHeader(requestHeader, op, variableMgr);
        }
        if (op.hasUrl()) {
            url = op.getUrl();
            url = variableMgr.updateReference(url);
        }

        if (!requestHeader.containsKey("accept-charset"))
            requestHeader.put("accept-charset", "utf-8");
        if (op.hasAccept())
            requestHeader.put("accept", op.getAccept());

        // http://build.fhir.org/testscript-definitions.html#TestScript.setup.action.operation.params
        if (op.hasUrl()) {
            String theUrl = variableMgr.updateReference(op.getUrl());
            if (theUrl == null)
                return;
            ref = new Ref(theUrl);
        } else if (op.hasParams()) {
            if (op.hasResource()) {
                String params = op.getParams();
                params = variableMgr.updateReference(params);
                if (params.startsWith("/"))
                    params = params.substring(1);  // should only be ID and _format (this is a READ)
                ref = new Ref(base, op.getResource(), params);
            } else {
                Reporter.reportError(val, opReport, null, type, label, "Resource (" + op.getResource() + ") specified but no params holding ID");
                return;
            }
        }
        else if (op.hasTargetId()) {
            ref = refFromTargetId(op.getTargetId(), opReport, label);
        } else if (fixtureMgr.getLastOp() != null) {
            ref = refFromTargetId(fixtureMgr.getLastOp(), opReport, label);
        }
        if (ref == null) {
            Reporter.reportError(val, opReport, null, type, label, "Unable to construct URL for operation");
            return;
        }
        if (ref.getResourceType().equals("")) {
            Reporter.reportError(val, opReport, null, type, label, "no resource type");
            return;
        }
        ResourceWrapper wrapper = fhirClient.readResource(ref, requestHeader);
        if (!wrapper.isOk()) {
            Reporter.reportError(val, opReport, null, type, label, "Unable to retrieve " + ref);
            return;
        } else {
            reporter.report(wrapper.getRef() + " read");
        }

        String fixtureId =op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        FixtureComponent fixtureComponent =  new FixtureComponent(fixtureId).setResource(wrapper);
        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    private Ref refFromTargetId(String targetId, TestReport.SetupActionOperationComponent opReport, String label) {
        Ref ref = null;
        FixtureComponent fixture  = fixtureMgr.get(targetId);
        if (fixture != null && fixture.hasHttpBase()) {
            String location = null;
            if (fixture.getHttpBase() instanceof HttpPost) {
                location = ((HttpPost) fixture.getHttpBase()).getLocationHeader().getValue();
            } else if (fixture.getHttpBase() instanceof HttpGet) {
                location = fixture.getHttpBase().getUri().toString();
            }
            if (location == null) {
                Reporter.reportError(val, opReport, null, type, label, "targetId does not have id and type");
                return null;
            }
            Ref targetRef = new Ref(location);
            if (base == null)
                ref = targetRef;
            else
                ref = targetRef.rebase(base);
        }
        return ref;
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
