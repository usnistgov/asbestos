package gov.nist.asbestos.mhd.channel;

public enum CanonicalUriCodeEnum {
    COMPREHENSIVE( UriCodeTypeEnum.PROFILE, "comprehensive"),
    MINIMAL(UriCodeTypeEnum.PROFILE , "minimal" ),
    SUBMISSIONSET(UriCodeTypeEnum.EXTENSION, "submissionset" ),
    IHESOURCEIDEXTENSION(UriCodeTypeEnum.EXTENSION ,""),
    IHEDESIGNATIONTYPEEXTENSIONURL(UriCodeTypeEnum.EXTENSION,"");

    private UriCodeTypeEnum type;
    private String code;

    CanonicalUriCodeEnum(UriCodeTypeEnum uriCodeTypeEnum, String code) {
        this.type = uriCodeTypeEnum;
        this.code = code;
    }

    public String getCode() {
            return code;
    }

    public UriCodeTypeEnum getType() {
        return type;
    }
}
