package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.Ref;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;

class OperationURLBuilder {

    static Ref build(TestScript.SetupActionOperationComponent op, URI base, FixtureMgr fixtureMgr, Reporter reporter, String resourceType) {
        Ref ref = null;
        // http://build.fhir.org/testscript-definitions.html#TestScript.setup.action.operation.params
        if (op != null && op.hasUrl()) {
            ref = new Ref(op.getUrl());
        } else if (op != null && op.hasParams()) {
            if (op.hasResource()) {
                String params = op.getParams();
                if (params.startsWith("/"))
                    params = params.substring(1);  // should only be ID and _format (this is a READ)
                ref = new Ref(base, op.getResource(), params);
            } else {
                reporter.reportError("Resource (" + op.getResource() + ") specified but no params holding ID");
                return null;
            }
        } else if (op != null && op.hasTargetId()) {
            FixtureComponent fixture = fixtureMgr.get(op.getTargetId());
            if (fixture != null && fixture.hasHttpBase()) {
                Ref targetRef = new Ref(fixture.getHttpBase().getUri());
                ref = targetRef.rebase(base);
            }
        } else if (base != null) {
            ref = new Ref(base);
            if (resourceType != null)
                ref = ref.withResource(resourceType);
        }
        if (ref == null) {
            reporter.reportError("Unable to construct URL for operation");
            return null;
        }
        return ref;
    }
}
