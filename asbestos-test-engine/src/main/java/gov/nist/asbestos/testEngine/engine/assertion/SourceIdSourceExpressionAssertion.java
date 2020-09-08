package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.EventLinkToUILink;
import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

public class SourceIdSourceExpressionAssertion {

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
        String expression = ctx.getVariableMgr().updateReference(ctx.getCurrentAssert().getCompareToSourceExpression());
        String found;
        try {
            found = FhirPathEngineBuilder.evalForString(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(ctx, "Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }

        String uiLink = EventLinkToUILink.get(sourceFixture.getCreatedByUIEvent(), "resp");
        fixtureLabels.setRawReference(uiLink);
        fixtureLabels.referenceLabel = sourceFixture.getId() == null ? "Open in Inspector" : sourceFixture.getId();

        AssertionReport.build(ctx.getCurrentAssertReport(), expression, fixtureLabels, sourceFixture);
        if ("true".equals(found))
            return Reporter.reportPass(ctx, expression);

        return Reporter.reportFail(ctx, "assertion failed - " + expression + " ==> " + found);

    }
}
