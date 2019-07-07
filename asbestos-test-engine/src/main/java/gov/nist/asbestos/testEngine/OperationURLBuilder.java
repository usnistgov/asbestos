package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.resolver.Ref;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.util.Map;

class OperationURLBuilder {

    static Ref build(TestScript.SetupActionOperationComponent op, URI base, Map<String, FixtureComponent> fixtures, Reporter reporter) {
        Ref ref = null;
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
                reporter.reportError("Resource (" + op.getResource() + ") specified but no params holding ID");
                return null;
            }
        } else if (op.hasTargetId()) {
            FixtureComponent fixture = fixtures.get(op.getTargetId());
            if (fixture != null && fixture.hasHttpBase()) {
                Ref targetRef = new Ref(fixture.getHttpBase().getUri());
                ref = targetRef.rebase(base);
            }
        }
        if (ref == null) {
            reporter.reportError("Unable to construct URL for operation");
            return null;
        }
        return ref;
    }
}
