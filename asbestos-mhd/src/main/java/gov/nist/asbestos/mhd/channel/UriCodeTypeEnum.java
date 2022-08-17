package gov.nist.asbestos.mhd.channel;

public enum UriCodeTypeEnum {
    PROFILE("profile"),
    EXTENSION("extension");

    private String type;

    UriCodeTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
