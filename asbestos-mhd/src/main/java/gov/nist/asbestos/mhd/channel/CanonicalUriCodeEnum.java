package gov.nist.asbestos.mhd.channel;

public enum CanonicalUriCodeEnum {
    COMPREHENSIVE( "profile", "comprehensive"),
    MINIMAL("profile", "minimal" ),
    SUBMISSIONSET("extension", "submissionset" ),
    IHESOURCEIDEXTENSION("extension",""),
    IHEDESIGNATIONTYPEEXTENSIONURL("extension","");

    private String type;
    private String code;

    CanonicalUriCodeEnum(String type, String code) {
        this.type = type;
        this.code = code;
    }

    public String getCode() {
            return code;
    }

    public String getType() {
        return type;
    }
}
