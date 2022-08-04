package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;

public interface MhdProfileVersionInterface {
    String getIheReference();
    default MhdBundleProfile getBundleProfile(MhdBundleProfileEnum profileEnum) {
       for (MhdBundleProfile mbp : profile().getAll()) {
           if (mbp.getType().equals(profileEnum)) {
               return mbp;
           }
       }
       return null;
    }
    ProfileVersionCanonicalUri profile();
    default MhdBundleProfileEnum getBundleProfileType(Bundle bundle) {
        try {
            if (bundle.getMeta().getProfile().size() != 1) {
                return MhdBundleProfileEnum.UNEXPECTED_COUNT;
//            return new ValE("Profile not declared in bundle.").asError()
//                    .add(new ValE(profile.getIheReference()).asDoc());
            }

            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            for (MhdBundleProfile mbp : profile().getAll()) {
                if (mbp.getCanonicalUri().equals(bundleProfile.asStringValue())) {
                   return mbp.getType();
                }
            }
//                return new ValE("Do not understand profile declared in bundle - " + bundleProfile).asError()
//                        .add(new ValE(profile.getIheReference()).asDoc());
        } catch (Exception e) {
            return MhdBundleProfileEnum.ERROR;
//            return new ValE("Bundle.meta.profile missing").asError()
//                    .add(new ValE(profile.getIheReference()).asDoc());
        }
        return MhdBundleProfileEnum.UNRECOGNIZED;
    }
    List<Class<?>> getAcceptableResourceTypes();
    RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities);
    MhdTransforms getMhdTransforms();
    String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers);
}
