package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.channel.MhdVersionEnum;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MhdV410 implements MhdProfileVersionInterface {
    private final String BUNDLE_RESOURCES_DOC_REF = String.format("3.65.4.1.2.1 Bundle Resources. %s",  getDocBase("ITI-65.html#23654121-bundle-resources"));
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(ListResource.class, DocumentReference.class, Binary.class);
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv410;

    private Val val;
    private MhdTransforms mhdTransforms;
    private CanonicalUriCodeEnum mhdBundleProfileEnum;
    private static final Logger logger = Logger.getLogger(MhdV410.class.getName());

    public MhdV410(Bundle bundle, Val val, MhdTransforms mhdTransforms) {
        this.val = val;
        this.mhdTransforms = mhdTransforms;
        try {
            this.mhdBundleProfileEnum = getUriCodesClass().detectBundleProfileType(bundle).getKey();
        } catch (Exception ex) {
            this.mhdBundleProfileEnum = null;
            logger.warning("mhdBundleProfileEnum is null. Exception: " + ex );
        }
    }


    @Override
    public MhdVersionEnum getMhdVersion() {
        return mhdVersionEnum;
    }

    @Override
    public CanonicalUriCodeEnum getDetectedBundleProfile() {
        return mhdBundleProfileEnum;
    }

    @Override
    public String getIheReference() {
        return BUNDLE_RESOURCES_DOC_REF;
    }


    @Override
    public List<Class<?>> getAcceptableResourceTypes() {
        return acceptableResourceTypes;
    }

    /**
     * Builds a submission set only if FHIR List resource matches a submission set type.
     * Returns null if resource is not a submission set type.
     * @param wrapper
     * @param vale
     * @param idBuilder
     * @param channelConfig
     * @param codeTranslator
     * @param assigningAuthorities
     * @return
     */
    @Override
    public RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {

        /*
        if resource is of ListResource class type
            submissionset code must exist
                iterate codes
                must have an entry for
                         system value="https://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"/>
                        <code value="submissionset"/>
                only one submissionset bundle entry is allowed.

            create a  new method to buildRegistryPackageType
         */

        return new MhdV4Common(this, mhdTransforms).buildSubmissionSet( wrapper, val, vale, idBuilder, channelConfig, codeTranslator, assigningAuthorities);
    }



    @Override
    public MhdTransforms getMhdTransforms() {
        return mhdTransforms;
    }

    @Override
    public String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers) {
        return rMgr.allocateSymbolicId();
    }



}
