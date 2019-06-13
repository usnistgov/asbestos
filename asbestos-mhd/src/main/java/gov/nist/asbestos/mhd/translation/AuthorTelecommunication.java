package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;

class AuthorTelecommunication extends AuthorPart {
    // format is XTN
    String telecomAddr = null;
    String subscriberNumber = null;
    String phoneNumber = null;
    String countryCode = null;
    String cityCode = null;

    @Override
    void validate(Val val) {
        parse();
        if (!get(2).isEmpty() && !get(2).equals("NET"))
            val.add(new ValE("AuthorTelecommunication segment 2. if present must be NET - found " + get(2)).asError());
        if (!get(3).equals("Internet"))
            val.add(new ValE("AuthorTelecommunication segment 3 must be Internet - found " + get(3)).asError());
        if (get(4).equals(""))
            val.add(new ValE("AuthorTelecommunication segment 4 (telecommunications address) must be present").asError());
        if (get(7).equals(""))
            val.add(new ValE("AuthorTelecommunication segment 7 (Subscriber Number) must be present").asError());
        if (parts.length > 8)
            val.add(new ValE("Author segments above 8 shall not be present").asError());
        telecomAddr = get(4);
        subscriberNumber = get(7);
        phoneNumber = get(2);
        countryCode = get(5);
        cityCode = get(6);
    }

}
