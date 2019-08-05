package gov.nist.asbestos.mhd.transforms;

import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RegistryErrorListToOperationOutcome {
    private RegistryErrorList rel;

    public RegistryErrorListToOperationOutcome setRegistryErrorList(RegistryErrorList rel) {
        this.rel = rel;
        return this;
    }

    public OperationOutcome getOperationOutcome() {
        Objects.requireNonNull(rel);
        OperationOutcome oo = new OperationOutcome();
        Set<String> msgs = new HashSet<>();

        for (RegistryError re : rel.getRegistryError()) {
            String msg = re.getCodeContext();
            if (msgs.contains(msg))
                continue;
            msgs.add(msg);
            OperationOutcome.OperationOutcomeIssueComponent com = new OperationOutcome.OperationOutcomeIssueComponent();
            com.setDiagnostics(msg);
            com.setSeverity((isWarning(re.getSeverity())) ? OperationOutcome.IssueSeverity.WARNING : OperationOutcome.IssueSeverity.FATAL);
            com.setCode(OperationOutcome.IssueType.INVALID);
            oo.addIssue(com);
        }

        return oo;
    }

    private boolean isWarning(String status) {
        return status != null && status.endsWith("Warning");
    }
}
