package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class ExpressionAssertion {

    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;
        BaseResource sourceResource = sourceFixture.getResourceResource();
        if (sourceResource == null) {
            Reporter.reportError(ctx, "Fixture referenced <" + sourceFixture.getId()  + "> has no resource.");
            return false;
        }
        String rawExpression = ctx.getCurrentAssert().getExpression();
        String expression = ctx.getVariableMgr().updateReference(rawExpression);
        boolean ok;
        try {
            ok = FhirPathEngineBuilder.evalForBoolean(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(ctx,"Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }

        AssertionReport.build(ctx.getCurrentAssertReport(), null,
                rawExpression,  expression,
                ok, sourceFixture, fixtureLabels);
        if (ok)
            return Reporter.reportPass(ctx, expression);
        return Reporter.reportFail(ctx, "expression " + ctx.getCurrentAssert().getExpression()  +  " failed.");

    }
}
