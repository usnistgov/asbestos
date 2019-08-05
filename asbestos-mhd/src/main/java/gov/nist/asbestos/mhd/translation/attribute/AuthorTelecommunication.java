package gov.nist.asbestos.mhd.translation.attribute;

import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;

class AuthorTelecommunication extends AuthorPart {
    // format is XTN which is two formats, phone number OR email address

    // shared
    String type = "";

    // phone number
    String phoneNumber = "";
    String countryCode = "";
    String areaCode = "";
    String subscriberNumber = "";
    String extension = "";

    // email
    String telecomAddr = "";

    @Override
    void validate(Val val) {
        type = get(3);
        if (type.equals(""))
            val.add(new ValE("AuthorTelecommunication segment 3 must be present").asError());
        if (type.equals("PH")) {
            countryCode = get(5);
            areaCode = get(6);
            subscriberNumber = get(7);
            extension = get(8);
            if (subscriberNumber.equals(""))
                val.add(new ValE("AuthorTelecommunication segment 7 (as part of phone number) must be present").asError());
        } else if (type.equals("Internet")) {
            if (!get(2).equals("") && !get(2).equals("NET"))
                val.add(new ValE("AuthorTelecommunication segment 2 (as part of internet address) must be empty or NET").asError());
            telecomAddr = get(4);
            if (telecomAddr.equals(""))
                val.add(new ValE("AuthorTelecommunication segment 4 (as part of internet address) must be present").asError());
        } else {
            val.add(new ValE("AuthorTelecommunication segment 3 must be PH or Internet - found " + type).asError());
        }
    }

    boolean isFull() {
        return !type.equals("");
    }

    public void setInternet() {
        type = "Internet";
    }

    public void setPhone() {
        type = "PH";
    }

    public String toString() {
        if (type.equals("Internet"))
            return "^NET^Internet" + "^" + telecomAddr;
        else
            return "^^PH^^" + countryCode + "^" + areaCode + "^" + subscriberNumber + "^" + extension;
    }

}
