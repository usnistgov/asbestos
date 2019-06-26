package gov.nist.asbestos.utilities;

import org.hl7.fhir.r4.model.OperationOutcome;

public class OperationOutcomeBuilder {

    public static OperationOutcome build(RegErrorList regErrorList) {

        if (regErrorList.hasErrors() || regErrorList.hasWarnings()) {
            OperationOutcome oo = new OperationOutcome();

            for (RegError regError : regErrorList.getList()) {
                OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
                issue.setSeverity(regError.getSeverity() == ErrorType.Error
                        ? OperationOutcome.IssueSeverity.ERROR
                        : OperationOutcome.IssueSeverity.WARNING);
                oo.addIssue(issue);
            }

            return oo;
        }
        return null;
    }
}
