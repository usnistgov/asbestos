package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import java.util.List;

class AuthorRole extends AuthorPart {
    private CodeableConcept cc = null;
    // format is string or coded string (code^^^&CodeSystemID&ISO)
    private String code = null;
    private String codeSystemId = null;
    private static final String SNOMED_OID = "2.16.840.1.113883.6.96";
    private static final String SNOMED_URL = "http://snomed.info/sct";

    public AuthorRole() {

    }

    public AuthorRole(CodeableConcept cc) {
        this.cc = cc;
        codeAndSystemFromCc();
    }

    @Override
    void validate(Val val) {

    }

    public CodeableConcept getCodeableConcept() {
        if (hasCodeableConcept())
            return cc;
        if (hasCodeAndSystem()) {
            if (codeSystemId.equals(SNOMED_OID))
                cc = new CodeableConcept().addCoding(new Coding().setCode(code).setSystem(SNOMED_URL));
            cc = new CodeableConcept().addCoding(new Coding().setDisplay(codeSystemId + "|" + code));
        }
        return cc;
    }

    public void setCc(CodeableConcept cc) {
        this.cc = cc;
    }

    public boolean hasCodeAndSystem() {
        return codeSystemId != null && code != null;
    }

    public String getCodeAndSystem() {
        if (hasCodeAndSystem())
            return code + "^^^&" + codeSystemId + "&ISO";
        return null;
    }

    public void setCodeAndSystem(String codeAndSystem) {
        String[] parts = codeAndSystem.split("^");
        if (parts.length == 4) {
            code = parts[0];
            String[] parts2 = parts[3].split("&");
            if (parts2.length == 3) {
                codeSystemId = parts2[1];
            }
        }
    }

    public boolean hasCodeableConcept() {
        return cc != null;
    }

    public boolean hasValue() {
        return hasCodeableConcept() || hasCodeAndSystem();
    }

    // TODO can have multiple roles
    private void codeAndSystemFromCc() {
        if (hasCodeableConcept()) {
            if (cc.hasCoding()) {
                code = cc.getCoding().get(0).getCode();
                String system = cc.getCoding().get(0).getSystem();
                if (SNOMED_URL.equals(system)) {
                    codeSystemId = SNOMED_OID;
                }
            }
        }
    }
}
