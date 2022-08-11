package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.mhd.transforms.MhdV3x;
import gov.nist.asbestos.mhd.transforms.MhdV3xCanonicalUriCodes;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.mhd.transforms.MhdV410;
import gov.nist.asbestos.mhd.transforms.MhdV410CanonicalUriCodes;
import gov.nist.asbestos.mhd.transforms.MhdV4CanonicalUriCodes;

import java.util.Objects;

/**
 * @author skb1
 */
public enum MhdVersionEnum {
    MHDv3x("MHDv3.x", MhdV3x.class, MhdV3xCanonicalUriCodes.class, "https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD_Rev3-2_TI_2020-08-28.pdf"), // or https://profiles.ihe.net/ITI/MHD/history.html
    MHDv4("MHDv4", MhdV4.class, MhdV4CanonicalUriCodes.class, "https://profiles.ihe.net/ITI/MHD/4.0.1"),
    MHDv410("MHDv410", MhdV410.class, MhdV410CanonicalUriCodes.class, "https://profiles.ihe.net/ITI/MHD/4.1.0" );

    private String version;
    private String mhdDocBase;
    private Class<? extends MhdProfileVersionInterface> mhdImplClass;
    private Class<? extends MhdCanonicalUriCodeInterface> uriCodesClass;

    MhdVersionEnum(String version,  Class<? extends MhdProfileVersionInterface> mhdImplClass, Class<? extends MhdCanonicalUriCodeInterface> mhdCanonicalUriImplClass, String mhdDocBase) {
        this.version = version;
        this.mhdDocBase = mhdDocBase;
        this.mhdImplClass = mhdImplClass;
        this.uriCodesClass = mhdCanonicalUriImplClass;
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

    public String getMhdDocBase() {
        return mhdDocBase;
    }

    public Class<? extends MhdProfileVersionInterface> getMhdImplClass() {
        return mhdImplClass;
    }

    public Class<? extends MhdCanonicalUriCodeInterface> getUriCodesClass() {
        return uriCodesClass;
    }
}
