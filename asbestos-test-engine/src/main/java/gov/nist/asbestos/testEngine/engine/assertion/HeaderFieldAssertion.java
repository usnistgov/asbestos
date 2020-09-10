package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class HeaderFieldAssertion {

    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;
        FixtureComponent sourceFixture = ctx.getSource();
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        String sourceHeaderFieldValue = sourceFixture
                .getHttpBase()
                .getResponseHeaders()
                .getValue(ctx.getCurrentAssert().getHeaderField());
        AssertionReport.build(ctx.getCurrentAssertReport(), "Header field " + ctx.getCurrentAssert().getHeaderField(),
                ctx.getCurrentAssert().getValue(),
                sourceHeaderFieldValue,
                sourceFixture,
                fixtureLabels);

        if (sourceHeaderFieldValue.equalsIgnoreCase(ctx.getCurrentAssert().getValue()))
            return Reporter.reportPass(ctx, sourceHeaderFieldValue + " = " + ctx.getCurrentAssert().getValue());
        return Reporter.reportFail(ctx, sourceHeaderFieldValue + " != " + ctx.getCurrentAssert().hasValue());

    }
}
