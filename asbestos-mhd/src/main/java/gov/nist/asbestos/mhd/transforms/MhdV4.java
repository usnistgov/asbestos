package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ListResource;

import java.util.Arrays;
import java.util.List;

public class MhdV4 implements MhdProfileVersionInterface {
    static String comprehensiveMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle";
    static String minimalMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle";
    /**
     * TODO
     */
    static String containedMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.UnContained.Comprehensive.ProvideBundle"; // TODO
    private static List<String> profiles = Arrays.asList(comprehensiveMetadataProfile, minimalMetadataProfile);
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(ListResource.class, DocumentReference.class, Binary.class);
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv4;

    private Val val;
    private MhdTransforms mhdTransforms;
    Boolean isMinimalMetadata = null;

    public MhdV4(Val val, MhdTransforms mhdTransforms) {
        this.val = val;
        this.mhdTransforms = mhdTransforms;
    }


    @Override
    public MhdVersionEnum getMhdVersionEnum() {
        return mhdVersionEnum;
    }

    @Override
    public boolean isBundleProfileDetected(Bundle bundle) {
        if (bundle.getMeta().getProfile().size() == 1) {
            try {
                CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
                return (profiles.contains(bundleProfile.asStringValue()));
            } catch (Exception ex) {
            }
        }
        return false;

    }

    /**
     *
     * @param bundle
     */
    @Override
    public void evalBundleProfile(Bundle bundle) {
        String docRef = "3.65.4.1.2.1 Bundle Resources. See https://profiles.ihe.net/ITI/MHD/ITI-65.html#23654121-bundle-resources";
        if (bundle.getMeta().getProfile().size() != 1)
            val.add(new ValE("No profile declaration present in bundle").asError()
                    .add(new ValE(docRef).asDoc()));
        try {
            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            if (!profiles.contains(bundleProfile.asStringValue()))
                val.add(new ValE("Do not understand profile declared in bundle - " + bundleProfile).asError()
                        .add(new ValE(docRef).asDoc()));
            if (bundleProfile.asStringValue().equals(minimalMetadataProfile))
                isMinimalMetadata = new Boolean(true);
        } catch (Exception e) {
            val.add(new ValE("Bundle.meta.profile missing").asError()
                    .add(new ValE(docRef).asDoc()));
        }
    }

    @Override
    public Boolean isMinimalMetadata() throws Exception {
        return isMinimalMetadata;
    }

    @Override
    public List<Class<?>> getAcceptableResourceTypes() {
        return acceptableResourceTypes;
    }

    @Override
    public RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        return null;
        /*
        if resource is of ListResource class type
            code must exist
            iterate codes
                must have an entry for
                         system value="http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"/>
                        <code value="submissionset"/>
                only one submissionset bundle entry is allowed.

                create a  new method to buildRegistryPackageType

         */
    }


}
