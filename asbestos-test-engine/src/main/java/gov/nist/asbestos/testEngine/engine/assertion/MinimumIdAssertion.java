package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;

import static gov.nist.asbestos.testEngine.engine.AssertionRunner.*;

public class MinimumIdAssertion {

    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;
        FixtureComponent sourceFixture = ctx.getSource();

        FixtureComponent miniFixture  = ctx.getFixtureMgr().get(ctx.getCurrentAssert().getMinimumId());
        if (miniFixture == null) {
            Reporter.reportError(ctx, "minimumId references fixture " + ctx.getCurrentAssert().getMinimumId() + " which cannot be found.");
            return false;
        }

        BaseResource miniR = miniFixture.getResourceResource();   // reference
        BaseResource sourceR = sourceFixture.getResourceResource();  // sut
        if (sourceR == null) {
            Reporter.reportFail(ctx, "No source");
            return false;
        }

        Class<?> sourceClass = sourceR.getClass();

        ctx.getCurrentAssertReport().setUserData(EVALUATING_TYPE, sourceClass.getSimpleName());   // resource type being evaluated
        ctx.getCurrentAssertReport().setUserData(SCRIPT, ctx.getCurrentAssert().getLabel());   // assert label (documentation only - nothing computed)

        MinimumId.Report report = new MinimumId().run(miniR, sourceR, ctx.isRequest());
        if (!report.errors.isEmpty()) {
            ctx.getCurrentAssertReport().setUserData("No Comparison", report.errors.get(0));
            Reporter.reportFail(ctx, report.errors.get(0));
            return false;
        }

        // This UserData is a properties style list for private transport between components
        // It is used here to pass back the raw MinimumId.Report to the caller which might be AnalysisReport.java
        ctx.getCurrentAssertReport().setUserData(RAW_REPORT, report);

        ctx.getCurrentAssertReport().setDetail(
                "[" +
                        String.join(", ", report.expected)
                        + "]"
        );

        AssertionReport.build(ctx, sourceFixture, miniFixture);


        if (report.missing.isEmpty()) {
            Reporter.reportPass(ctx, "pass");
            return true;
        } else {
            String atts = String.join(", ", report.missing);
            Reporter.reportFail(ctx, "attributes [" + atts + "] not found ");
            return false;
        }
    }
}
