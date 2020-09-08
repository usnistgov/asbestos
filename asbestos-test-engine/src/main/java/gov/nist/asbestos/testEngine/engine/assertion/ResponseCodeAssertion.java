package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.AssertionRunner;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;

public class ResponseCodeAssertion {

    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;

        int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
        String found = String.valueOf(codeFound);
        String expected = ctx.getCurrentAssert().getResponseCode();
        String operator = ctx.getCurrentAssert().hasOperator() ? ctx.getCurrentAssert().getOperator().toCode() : "equals";

        AssertionReport.build(ctx.getCurrentAssertReport(), "Response: ", expected, operator, found, sourceFixture, fixtureLabels);
        if (!AssertionRunner.compare(
                ctx,
                found,
                expected,
                operator
        ))
            return false;
        return Reporter.reportPass(ctx, found + " " + operator + " " + expected);
    }
}
