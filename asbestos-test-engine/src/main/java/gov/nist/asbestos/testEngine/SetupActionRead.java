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

class SetupActionRead {
    private FixtureMgr fixtureMgr;  // static fixtures and history of operations
    private TestScript.SetupActionOperationComponent op;
    private ValE val;
    private URI base;
    private TestReport testReport = null;
    private VariableMgr variableMgr = null;
    private FhirClient fhirClient = null;


    SetupActionRead(FixtureMgr fixtureMgr) {
        this.fixtureMgr = fixtureMgr;
        this.op = op;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent opReport) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(variableMgr);
        Objects.requireNonNull(testReport);
        Objects.requireNonNull(fhirClient);
        String type = "setup.read";
        val = new ValE(val).setMsg(type);

        String label = null;
        boolean encodeRequestUrl;
        Map<String, String> requestHeader = new HashMap<>();
        String sourceId = null;
        String targetId = null;  // responseId of a GET or sourceId of POST/PUT
        String url = null;
        Ref ref = null;

        if (op.hasLabel())
            label = op.getLabel();
        if (op.hasEncodeRequestUrl())
            encodeRequestUrl = op.getEncodeRequestUrl();
        if (op.hasRequestHeader()) {
            SetupActionCreate.handleRequestHeader(requestHeader, op, variableMgr);
        }
        if (op.hasSourceId())
            sourceId = op.getSourceId();
        if (op.hasTargetId())
            targetId = op.getTargetId();
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
            FixtureComponent fixture  = fixtureMgr.get(op.getTargetId());
            if (fixture != null && fixture.hasHttpBase()) {
                String location = null;
                if (fixture.getHttpBase() instanceof HttpPost) {
                    location = ((HttpPost) fixture.getHttpBase()).getLocationHeader().getValue();
                } else if (fixture.getHttpBase() instanceof HttpGet) {
                    location = ((HttpGet) fixture.getHttpBase()).getUri().toString();
                }
                if (location == null) {
                    Reporter.reportError(val, opReport, null, type, label, "targetId does not have id and type");
                    return;
                }
                Ref targetRef = new Ref(location);
                if (base == null)
                    ref = targetRef;
                else
                    ref = targetRef.rebase(base);
            }
        }
        if (ref == null) {
            Reporter.reportError(val, opReport, null, type, label, "Unable to construct URL for operation");
            return;
        }
        ResourceWrapper wrapper = fhirClient.readResource(ref, requestHeader);
        if (!wrapper.isOk()) {
            Reporter.reportError(val, opReport, null, type, label, "Unable to retrieve " + ref);
            return;
        }

        String fixtureId =op.hasResponseId() ? op.getResponseId() : FixtureComponent.getNewId();
        FixtureComponent fixtureComponent =  new FixtureComponent(fixtureId).setResource(wrapper);
        fixtureMgr.put(fixtureId, fixtureComponent);
    }

    SetupActionRead setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupActionRead setBase(URI base) {
        this.base = base;
        return this;
    }

    public SetupActionRead setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }

    public SetupActionRead setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    public SetupActionRead setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }
}
