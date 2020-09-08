package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.TestDef;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.TestReport;

import java.util.Objects;

public class AssertionReport {

    public static void build(TestReport.SetupActionAssertComponent assertReport, String desc, String expected, String found, FixtureComponent fixture, FixtureLabels fixtureLabels) {
        Reporter.assertDescription(assertReport, "**" + desc + "**: " +
                        "**Expected** " + expected + "  **Found** " + found +
                        " (**Source** " + fixtureLabels.getReference() + ")");
    }

    public static void build(TestReport.SetupActionAssertComponent assertReport, String desc, String expected, String operator, String found, FixtureComponent fixture, FixtureLabels fixtureLabels) {
        Objects.requireNonNull(fixtureLabels);
        fixtureLabels.referenceLabel = "Response";
        Reporter.assertDescription(assertReport, "**" + desc + "**: " +
                        "**Expected** " + expected + "  **Operator** " + operator + " **Found** " + found +
                        " (**Source** " + fixtureLabels.getReference() + ")");
    }

    public static void build(TestReport.SetupActionAssertComponent assertReport, String desc, String value, String expected, String operator, String found, FixtureComponent fixture, FixtureLabels fixtureLabels) {
        Reporter.assertDescription(assertReport, "**" + desc + "**: " + value +
                " **Expected** " + expected + "  **Operator** is " + operator + " **Found** " + found);
    }

    public static void build(TestReport.SetupActionAssertComponent assertReport, String descHdr, String raw, String expanded, boolean result, FixtureComponent fixture, FixtureLabels fixtureLabels) {
        Reporter.assertDescription(assertReport,
                (descHdr == null ? "" : "**" + descHdr + "**:" )+
                        "\n**Raw expression** " + raw + "\n**Expanded Expression** " + expanded +
                        "\n**Result** " + result);
    }

    public static void build(TestReport.SetupActionAssertComponent assertReport, String expression, FixtureLabels fixtureLabels, FixtureComponent fixture) {
        Reporter.assertDescription(assertReport, "**Eval** " + expression + " **against** " + fixtureLabels.getReference());
    }

    public static void build(AssertionContext ctx, FixtureComponent source, FixtureComponent reference) {
        FixtureLabels sourceLabels = new FixtureLabels(ctx.getTestDef(), source, FixtureLabels.Source.RESPONSE);
        sourceLabels.setReference(source);
        FixtureLabels referenceLabels = new FixtureLabels(ctx.getTestDef(), reference, FixtureLabels.Source.SOURCE);
        referenceLabels.setReference(reference);
        Reporter.assertDescription(ctx.getCurrentAssertReport(), "**Compare** " + sourceLabels.getReference() + " to **Reference** " + referenceLabels.getReference());
    }

}
