package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;

public class ContentTypeAssertion {

    // TODO: this and instResource cannot both be right, sourceFixture.getResponseType() ?
    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;
        AssertionReport.build(ctx.getCurrentAssertReport(),
                "Content type",
                ctx.getCurrentAssert().getContentType(),
                sourceFixture.getResponseType(),
                sourceFixture,
                fixtureLabels);

        if (ctx.getCurrentAssert().getContentType().equalsIgnoreCase(sourceFixture.getResponseType()))
            return Reporter.reportPass(ctx,
                    ctx.getCurrentAssert().getContentType() + " = " + sourceFixture.getResponseType());
        return Reporter.reportFail(ctx,
                "expecting " + ctx.getCurrentAssert().getContentType() + " found " + sourceFixture.getResponseType());

    }
}
