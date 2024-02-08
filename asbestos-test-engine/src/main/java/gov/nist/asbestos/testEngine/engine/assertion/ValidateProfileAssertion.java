package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.testEngine.engine.ValidationClient;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.TestReport.TestReportActionResult;

import java.io.IOException;

public class ValidateProfileAssertion {

    public static boolean run(AssertionContext ctx) {
        if (!ctx.validate())
            return false;

        FixtureComponent sourceFixture = ctx.getSource();

        String profile = ctx.getProfile(ctx.getCurrentAssert().getValidateProfileId());
        if (profile == null) {
            Reporter.reportFail(ctx, "Profile not found for profileId " + ctx.getCurrentAssert().getValidateProfileId());
            return false;
        }

        BaseResource sourceR = sourceFixture.getResourceResource();  // sut
        if (sourceR == null) {
            Reporter.reportFail(ctx, "No source");
            return false;
        }

        // change to access raw fixture in the future
        String content;
        try {
            content = new org.hl7.fhir.r4.formats.JsonParser().composeString((Resource) sourceR);
        } catch (IOException e) {
            Reporter.reportFail(ctx, "serializing failed for fixture " + sourceFixture.getId());
            return false;
        }

        ValidationClient client = ((TestEngine) ctx.getTestDef()).getValidationClient();
        OperationOutcome outcome = (OperationOutcome) client.validate(content, profile);

        org.hl7.fhir.r4.model.TestReport.SetupActionAssertComponent assertReport = ctx.getCurrentAssertReport();
    
        assertReport.setResult(TestReportActionResult.PASS);
        assertReport.setMessage("Validation of profile "+profile+ "for fixture "+sourceFixture.getId());
        assertReport.setDetail(outcome.getIssueFirstRep().getDiagnostics());
        
        Extension extension = assertReport.addExtension();
        extension.setUrl("http://hl7.org/fhir/StructureDefinition/referencesContained");
        extension.setValue(new org.hl7.fhir.r4.model.Reference("#"+outcome.getId()));
        ctx.getTestReport().addContained(outcome);
        
        int failures = getValidationFailures(outcome);
        if (getValidationWarnings(outcome)>0) {
            assertReport.setResult(TestReportActionResult.WARNING);
        }
        if (getValidationFailures(outcome)>0) {
            assertReport.setResult(TestReportActionResult.FAIL);
        }

        if (failures == 0) {
            Reporter.reportPass(ctx, "pass");
            return true;
        } else {
            Reporter.reportFail(ctx, "validation failed for profile "+profile);
            return false;
        }
    }

    public static void build(AssertionContext ctx, FixtureComponent source, FixtureComponent reference) {
        FixtureLabels sourceLabels = new FixtureLabels(ctx.getTestDef(), source, FixtureLabels.Source.RESPONSE);
        sourceLabels.setReference(source);
        FixtureLabels referenceLabels = new FixtureLabels(ctx.getTestDef(), reference, FixtureLabels.Source.REQUEST);
        referenceLabels.setReference(reference);
        String sourceRef = sourceLabels.getReference();
        if (sourceLabels.getRawReference() == null) {
            if (sourceLabels.getLabel() != null) {
                sourceRef = sourceLabels.getLabel();
            } else {
                sourceRef = "(fixture)";
            }
        }
        Reporter.assertDescription(ctx.getCurrentAssertReport(), "**Compare** " + sourceRef + " to **Reference** " + referenceLabels.getReference());
    }

    static public int getValidationWarnings(OperationOutcome outcome) {
		int fails = 0;
		if (outcome != null && outcome.getIssue() != null) {
			for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
				if (IssueSeverity.WARNING == issue.getSeverity()) {
					++fails;
				}
			}
		}
		return fails;
	}

    static public int getValidationFailures(OperationOutcome outcome) {
		int fails = 0;
		if (outcome != null && outcome.getIssue() != null) {
			for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
				if (IssueSeverity.FATAL == issue.getSeverity()) {
					++fails;
				}
				if (IssueSeverity.ERROR == issue.getSeverity()) {
					++fails;
				}
			}
		}
		return fails;
	}

}
