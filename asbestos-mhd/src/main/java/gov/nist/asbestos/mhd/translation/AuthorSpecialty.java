package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.simapi.validation.Val;

class AuthorSpecialty extends AuthorPart {
    // format is string or coded string (code^^^&CodeSystemID&ISO)
    PatientId format = null;

    @Override
    void validate(Val val) {
        if (value.contains("^")) {
            format = new PatientId();
            format.setPatientid(value);
        }
    }
}
