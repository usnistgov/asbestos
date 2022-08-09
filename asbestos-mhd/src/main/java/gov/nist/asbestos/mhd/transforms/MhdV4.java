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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Mappings for XDS and MHD Mapping (XDS)
 * MHD 4.0.1
 * SubmissionSet
 * List	XDS SubmissionSet
 *    meta
 *       profile	SubmissionSet.limitedMetadata
 *    extension (designationType)	SubmissionSet.contentTypeCode
 *    extension (sourceId)	SubmissionSet.sourceId
 *    (IGNORED MAPPING) extension (intendedRecipient)	SubmissionSet.intendedRecipient
 *    identifier	SubmissionSet.entryUUID and SubmissionSet.uniqueId
 *    status	SubmissionSet.availabilityStatus
 *    mode	shall be 'working'
 *    title	SubmissionSet.title
 *    code	shall be 'submissionset'
 *    subject	SubmissionSet.patientId
 *    date	SubmissionSet.submissionTime
 *    (IGNORED) source	SubmissionSet.author
 *       extension (authorOrg)	SubmissionSet.author when the author is an Organization
 *    (IGNORED) note	SubmissionSet.comments
 *    (IGNORED?) entry is [0..1]
 *       item	references to DocumentReference(s) and Folder List(s)
 *       (To be verified by the DocumentSource tests and Inspector Inspect Request, ie. Assertion ID FTK4RM500 ?)
 *
 *      TODO - Contained option
 *      static String containedMetadataProfile = "http://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition/IHE.MHD.UnContained.Comprehensive.ProvideBundle";
 */
public class MhdV4 implements MhdProfileVersionInterface {
    private final String BUNDLE_RESOURCES_DOC_REF = String.format("3.65.4.1.2.1 Bundle Resources. %s",  getDocBase("ITI-65.html#23654121-bundle-resources"));
    private static String comprehensiveMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle";
    private static String minimalMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle";
    private static final Map<CanonicalUriCodeEnum, String> canonicalUriCodeEnumStringMap =
        Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.SUBMISSIONSET, "http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"),
                new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.COMPREHENSIVE, comprehensiveMetadataProfile),
                new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.MINIMAL, minimalMetadataProfile),
                new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION, "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId"),
                new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL, "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType"))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(ListResource.class, DocumentReference.class, Binary.class);
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv4;

    private Val val;
    private MhdTransforms mhdTransforms;
    private CanonicalUriCodeEnum mhdBundleProfileEnum;
    private static final Logger logger = Logger.getLogger(MhdV4.class.getName());

    public MhdV4(Bundle bundle, Val val, MhdTransforms mhdTransforms) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(mhdTransforms);
        this.val = val;
        this.mhdTransforms = mhdTransforms;
        try {
            this.mhdBundleProfileEnum = detectBundleProfileType(bundle);
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

    /**
     * Hides interface static method
     * @return
     */
    public static Map<CanonicalUriCodeEnum, String> getProfiles() {
        return canonicalUriCodeEnumStringMap.entrySet().stream()
                .filter(e -> "profile".equals(e.getKey().getType())).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    /**
     * Hides interface static method
     * @return
     */
    public static Map<CanonicalUriCodeEnum, String> getAll() {
        return canonicalUriCodeEnumStringMap;
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
                         system value="http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"/>
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

