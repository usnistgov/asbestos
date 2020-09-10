package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class ResourceAssertion {


    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;
        FixtureComponent sourceFixture = ctx.getSource();
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
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
