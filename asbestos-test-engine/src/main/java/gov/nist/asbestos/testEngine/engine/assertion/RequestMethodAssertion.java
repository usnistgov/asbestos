package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class RequestMethodAssertion {

    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;
        FixtureComponent sourceFixture = ctx.getSource();
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        String requestedMethod = ctx.getCurrentAssert().getRequestMethod().toCode();
        String method = sourceFixture.getResourceWrapper().getHttpBase().getVerb();

        AssertionReport.build(ctx.getCurrentAssertReport(), "Request Method", requestedMethod, method, sourceFixture, fixtureLabels);
        if (requestedMethod.equalsIgnoreCase(method)) {
            Reporter.reportPass(ctx, "Method " + requestedMethod + " found");
            return true;
        }
        Reporter.reportFail(ctx, "Expected method " + requestedMethod + " found " + method);
        return false;

    }
}
