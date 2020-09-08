package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.testEngine.engine.AssertionRunner;
import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class ExpressionValueAssertion {

    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;

        BaseResource sourceResource = sourceFixture.getResourceResource();
        if (sourceResource == null) {
            Reporter.reportError(ctx,"Fixture referenced <" + sourceFixture.getId()  + "> has no resource");
            return false;
        }
        String expression = ctx.getVariableMgr().updateReference(ctx.getCurrentAssert().getExpression());
        String found;
        try {
            found = FhirPathEngineBuilder.evalForString(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(ctx,"Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }

        if (found != null && found.contains("/")) {
            Ref foundRef = new Ref(found);
            if (foundRef.getBase().toString().equals("")) {
                String contentLocation = sourceFixture.getHttpBase().getContentLocation();
                if (contentLocation != null && !contentLocation.equals("")) {
                    Ref locationRef = new Ref(contentLocation);
                    foundRef = foundRef.rebase(locationRef);
                    found = foundRef.toString();
                }
            }
        }

        // remove semantic name from status for comparison
        if (expression.endsWith("response.status")) {
            String[] parts = found.split(" ");
            if (parts.length > 1) {
                found = parts[0];
            }
        }

        String expected = ctx.getVariableMgr().updateReference(ctx.getCurrentAssert().getValue());
        String operator = ctx.getCurrentAssert().hasOperator() ? ctx.getCurrentAssert().getOperator().toCode() : "equals";

        AssertionReport.build(ctx.getCurrentAssertReport(), "Eval expression" , expression,
                expected, operator, found, sourceFixture, fixtureLabels);
        if (!AssertionRunner.compare(ctx, found, expected, operator))
            return false;
        return Reporter.reportPass(ctx, expression);
    }
}
