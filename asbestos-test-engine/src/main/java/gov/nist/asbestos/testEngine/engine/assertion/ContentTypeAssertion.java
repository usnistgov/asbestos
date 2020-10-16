package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class ContentTypeAssertion {

    // TODO: this and instResource cannot both be right, sourceFixture.getResponseType() ?
    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;
        FixtureComponent sourceFixture = ctx.getSource();
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        String actualValue = sourceFixture.getHttpBase().getResponseContentType();
        AssertionReport.build(ctx.getCurrentAssertReport(),
                "Content type",
                ctx.getCurrentAssert().getContentType(),
                actualValue,
                sourceFixture,
                fixtureLabels);

        if (ctx.getCurrentAssert().getContentType().equalsIgnoreCase(actualValue))
            return Reporter.reportPass(ctx,
                    ctx.getCurrentAssert().getContentType() + " = " + actualValue);
        return Reporter.reportFail(ctx,
                "expecting " + ctx.getCurrentAssert().getContentType() + " found " + actualValue);

    }
}
