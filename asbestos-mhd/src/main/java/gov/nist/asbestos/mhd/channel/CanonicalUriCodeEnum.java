package gov.nist.asbestos.mhd.channel;

public enum CanonicalUriCodeEnum {
    COMPREHENSIVE( "profile", "comprehensive"),
    MINIMAL("profile", "minimal" ),
    SUBMISSIONSET("extension", "submissionset" );

    private String code;
    private String type;

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
