package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;

class AuthorInstitution extends AuthorPart {
    // datatype is XON
    String orgName = null;
    String assigningAuthority = null;
    String orgId = null;

    @Override
    void validate(Val val) {
        parse();
        orgName = get(1);
        assigningAuthority = get(6);
        // TODO validate OID and ISO portion of aa

        if (orgName.isEmpty())
            val.add(new ValE("AuthorInstitution Organization Name must be present.").asError());

        // TODO validate orgId and higher indexes not present
    }

    public String getOrgName() {
        return orgName;
    }

    public String toString() {
        return orgName;
    }
}
