package gov.nist.asbestos.mhd.channel;


import java.util.List;

public interface ProfileVersionCanonicalUri {
    MhdVersionEnum getMhdVersion();
    List<MhdBundleProfile> getAll();
}
