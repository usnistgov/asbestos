package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.context.FhirContext;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupActionRead {
    private Map<String, FixtureComponent> fixtures;  // static fixtures and history of operations
    private TestScript.SetupActionOperationComponent op;
    private ValE val;
    private URI base;
    private FhirClient fhirClient;

    SetupActionRead(Map<String, FixtureComponent> fixtures, TestScript.SetupActionOperationComponent op) {
        this.fixtures = fixtures;
        this.op = op;
    }

    ResourceWrapper run() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(fhirClient);
        val = new ValE(val).setMsg("setup.read");

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
            List<TestScript.SetupActionOperationRequestHeaderComponent> hdrs = op.getRequestHeader();
            for (TestScript.SetupActionOperationRequestHeaderComponent hdr : hdrs) {
                requestHeader.put(hdr.getField(), hdr.getValue());
            }
        }
        if (op.hasSourceId())
            sourceId = op.getSourceId();
        if (op.hasTargetId())
            targetId = op.getTargetId();
        if (op.hasUrl())
            url = op.getUrl();


        if (!requestHeader.containsKey("accept-charset"))
            requestHeader.put("accept-charset", "utf-8");
        if (op.hasAccept())
            requestHeader.put("accept", op.getAccept());

        // http://build.fhir.org/testscript-definitions.html#TestScript.setup.action.operation.params
        if (op.hasUrl()) {
            ref = new Ref(op.getUrl());
        } else if (op.hasParams()) {
            if (op.hasResource()) {
                String params = op.getParams();
                if (params.startsWith("/"))
                    params = params.substring(1);  // should only be ID and _format (this is a READ)
                ref = new Ref(base, op.getResource(), params);
            } else {
                val.add(new ValE("Resource (" + op.getResource() + ") specified but no params holding ID").asError());
                return null;
            }
        }
        else if (op.hasTargetId()) {
            FixtureComponent fixture  = fixtures.get(op.getTargetId());
            if (fixture != null) {
                Ref targetRef = fixture.getRef();
                if (targetRef != null)
                    ref = targetRef.rebase(base);
            }
        }
        if (ref == null) {
            val.add(new ValE("Unable to construct URL for operation").asError());
            return null;
        }
        ResourceWrapper wrapper = fhirClient.readResource(ref, requestHeader);

        if (op.hasResponseId())
            fixtures.put(op.getResponseId(), new FixtureComponent(op.getResponseId(), wrapper, null));

        return wrapper;
    }

    SetupActionRead setVal(ValE val) {
        this.val = val;
        return this;
    }

    public void setBase(URI base) {
        this.base = base;
    }

    public void setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
    }
}
