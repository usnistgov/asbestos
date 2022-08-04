package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;
import java.util.function.Supplier;

public interface MhdProfileVersionInterface {
    MhdVersionEnum getMhdVersionEnum();
    String getComprehensiveMetadataProfileCanonicalUri();
    String getMinimalMetadataProfileCanonicalUri();
    Supplier<String> getminpcrui();
    List<String> getProfileCanonicalUris();
    boolean isBundleProfileDetected(Bundle bundle);
    void evalBundleProfile(Bundle bundle);
    default void evalBundleProfile(Val val, Bundle bundle, List<String> profiles, String iheReference) {
        if (bundle.getMeta().getProfile().size() != 1)
            val.add(new ValE("Profile not declared in bundle.").asError()
                    .add(new ValE(iheReference).asDoc()));
        try {
            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            if (!profiles.contains(bundleProfile.asStringValue()))
                val.add(new ValE("Do not understand profile declared in bundle - " + bundleProfile).asError()
                        .add(new ValE(iheReference).asDoc()));
        } catch (Exception e) {
            val.add(new ValE("Bundle.meta.profile missing").asError()
                    .add(new ValE(iheReference).asDoc()));
        }
    }
    List<Class<?>> getAcceptableResourceTypes();
    RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities);
    MhdTransforms getMhdTransforms();
    String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers);
}
