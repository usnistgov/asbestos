package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;

class AuthorPerson extends AuthorPart {
    // datatype is XCN
    String id = "";
    String familyName = "";
    String givenName = "";
    String suffix = "";
    String prefix = "";
    String assigningAuthority = "";

    @Override
    void validate(Val val) {
        id = get(1);
        familyName = get(2);
        givenName = get(3);
        suffix = get(5);
        prefix = get(6);
        assigningAuthority = get(9);

        if (id == null && familyName == null)
            val.add(new ValE("AuthorPerson identifier or last name must be present.").asError());
        if (assigningAuthority != null) {
            String[] aparts = assigningAuthority.split("&");
            if (aparts.length != 3) {
                val.add(new ValE("AssigningAuthority is specified but does not have 3 parts separated by & character").asError());
                return;
            }
            if (!aparts[0].equals(""))
                val.add(new ValE("AssigningAuthority first sub-component shall be empty.").asError());
            if (!aparts[2].equals("ISO"))
                val.add(new ValE("AssigningAuthority third sub-component shall be ISO.").asError());
            // TODO validate oid in aparts[1]
        }
    }

    public String toString() {
        return id + "^" + familyName + "^" + givenName + "^^" + suffix + "^" + prefix + "^^^" + assigningAuthority;
    }
}
