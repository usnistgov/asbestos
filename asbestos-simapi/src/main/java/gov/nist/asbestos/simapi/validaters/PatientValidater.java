package gov.nist.asbestos.simapi.validaters;

import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValidationReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;


public class PatientValidater {
    private ValidationReport report;

    public PatientValidater(ValidationReport report) {
        this.report = report;
    }

    public void run(Patient patient) {
        Val top  = new Val();

        if (patient.getResourceType().name().equals("Patient")) {
            top.msg("Patient resource");
        } else {
            top.err(new Val("resourceType missing or not set to Patient"));
        }

        //
        // has at least one identifier with system and value
        //
        List<Identifier> identifiers = patient.getIdentifier();
        if (identifiers.size() > 0) {
            report.add(top.msg("Has " + identifiers.size() + " identifiers"));
            if (identifiers.size() == 0) {
                report.add(top.err(new Val("No identifiers")));
            }
        } else {
            report.add(top.err(new Val("No identifiers")));
        }
        if (identifiers.size() == 0) return;
        boolean hasProperIdentifier = false;
        for (Identifier identifier : identifiers) {
            if (isOid(identifier.getSystem()) && identifier.getValue() != null) {
                hasProperIdentifier = true;
            }
        }
        if (hasProperIdentifier)
            top.msg("Has identifier with value and OID valued system");
        else
            top.err(new Val("Does not have an identifier with OID value system"));
    }

    private boolean isOid(String value) {
        return value.startsWith("urn:oid:");
    }
}
