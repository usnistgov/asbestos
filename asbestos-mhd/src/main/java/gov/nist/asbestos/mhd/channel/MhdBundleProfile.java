package gov.nist.asbestos.mhd.channel;

public class MhdBundleProfile {
    private MhdBundleProfileEnum type;
    private String canonicalUri;

    public MhdBundleProfile(MhdBundleProfileEnum type, String canonicalUri ) {
        this.type = type;
        this.canonicalUri = canonicalUri;
    }

    public MhdBundleProfileEnum getType() {
        return type;
    }

    public String getCanonicalUri() {
        return canonicalUri;
    }

}
