package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;

public class ResourceAssertion {


    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;
        AssertionReport.build(ctx.getCurrentAssertReport(),
                "Resource type",
                ctx.getCurrentAssert().getResource(),
                sourceFixture.getResponseType(),
                sourceFixture,
                fixtureLabels);

        if (ctx.getCurrentAssert().getResource().equals(sourceFixture.getResponseType()))
            return Reporter.reportPass(ctx, "resource type comparison (" + sourceFixture.getResponseType() + ")" );
        return Reporter.reportFail(ctx, "expected " + ctx.getCurrentAssert().getResource() + " found " + sourceFixture.getResponseType());
    }
}
