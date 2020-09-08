package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;

public class RequestMethodAssertion {

    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;
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
