package gov.nist.asbestos.mhd.channel;

import java.util.Objects;

public enum MhdVersionEnum {
    MHDv3x("MHDv3.x", "MHD Profile - Rev 3.1. See https://profiles.ihe.net/ITI/MHD/history.html"),
    MHDv4("MHDv4", "MHD Profile - Rev 4.0.1. See https://profiles.ihe.net/ITI/MHD/4.0.1/toc.html"),
    MHDv410("MHDv410", "MHD Profile - Rev 4.1.0. See https://profiles.ihe.net/ITI/MHD/4.1.0/toc.html");

    private String version;
    private String mhdProfileRef;

    MhdVersionEnum(String version, String mhdProfileRef) {
        this.version = version;
        this.mhdProfileRef = mhdProfileRef;
    }

    static public MhdVersionEnum find(String s) {
        Objects.requireNonNull(s);
        for (MhdVersionEnum p : values()) {
            if (s.equals(p.version)) return p;
            try {
                if (p == MhdVersionEnum.valueOf(s)) return p;
            } catch (IllegalArgumentException e) {
                // continue;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return version;
    }

    public boolean equals(MhdVersionEnum p) {
        return (p.toString().equals(this.toString()));
    }

    public boolean equals(String s) {
        return (this.toString().equals(s));
    }

    public String getVersion() { return version; }

    public String getMhdProfileRef() {
        return mhdProfileRef;
    }
}
