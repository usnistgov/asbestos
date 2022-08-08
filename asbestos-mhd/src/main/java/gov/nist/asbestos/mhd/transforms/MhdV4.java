package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.channel.MhdVersionEnum;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.codesystems.ListMode;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private static final String SUBMISSION_SET_PROFILE_DOCREF = "https://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition-IHE.MHD.Minimal.SubmissionSet.html#profile";
    private static final String BUNDLE_RESOURCES_DOC_REF = "3.65.4.1.2.1 Bundle Resources. See https://profiles.ihe.net/ITI/MHD/4.0.1/ITI-65.html#23654121-bundle-resources";
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

        BaseResource resource = wrapper.getResource();
        vale.setMsg("The Document Recipient shall transform the Bundle content into a proper message for the Given grouped Actor. " +
                "(Document Recipient is grouped with XDS Document Source. " +
                "Transformation task: Transforming List to SubmissionSet.)" +
                "https://profiles.ihe.net/ITI/MHD/4.0.1/ITI-65.html#23654131-grouping-with-actors-in-other-document-sharing-profiles");

        if (MhdProfileVersionInterface.isCodedListType(Arrays.asList(getMhdVersion()), resource, "submissionset")) {
            return createSubmissionSet(idBuilder, wrapper, vale, channelConfig, codeTranslator, assigningAuthorities);
        }

        return null;
    }


    private RegistryPackageType createSubmissionSet(IdBuilder idBuilder, ResourceWrapper wrapper, ValE vale, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        ListResource listResource = (ListResource)wrapper.getResource();

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("SubmissionSet of ListResource type has Identifier (entryUUID)").asError()
                        .addIheRequirement("2:3.65.4.1.2 Message Semantics: " +
                                "The Document Source shall not provide any entryUUID values."
                        + "See https://profiles.ihe.net/ITI/MHD/4.0.1/ITI-65.html#2365412-message-semantics"));
        }

        RegistryPackageType ss = new RegistryPackageType();

        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + wrapper.getAssignedId() + ")"));
        ss.setId(wrapper.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (listResource.hasMode()) {
            if (!ListMode.WORKING.equals(listResource.getMode())) {
                vale.add(new ValE("Mode Required Pattern: working")
                .addIheRequirement(SUBMISSION_SET_PROFILE_DOCREF));
            }
        } else {
            vale.add(new ValE("Mode is required [1..1]").addIheRequirement(SUBMISSION_SET_PROFILE_DOCREF));
        }

        if (listResource.hasDate())
            mhdTransforms.addSlot(ss, "submissionTime", mhdTransforms.translateDateTime(listResource.getDate()));
        if (listResource.hasTitle()) {
            mhdTransforms.addName(ss, listResource.getTitle());
        }
        // 3bdd: Submission set type classification
        mhdTransforms.addClassification(ss, MhdTransforms.URN_UUID__BDD_SUBMISSION_SET, mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId());

        if (listResource.hasExtension(getAll().get(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL))) {
           Extension extension = listResource.getExtensionByUrl(getAll().get(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL));
           if (extension.getValue() instanceof CodeableConcept) {
              CodeableConcept codeableConcept = (CodeableConcept)extension.getValue();
               mhdTransforms.addClassificationFromCodeableConcept(ss, codeableConcept, CodeTranslator.CONTENTTYPECODE, wrapper.getAssignedId(), vale, codeTranslator);
           }
        }

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().size() < 2) {
               vale.add(new ValE("Minimum List identifier cardinality is less than 2. Should be 2..*")
                       .addIheRequirement("https://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition-IHE.MHD.Minimal.SubmissionSet.html"));
            } else {
                long systemIdCount = listResource.getIdentifier().stream()
                        .filter(e -> e.hasUse() && Identifier.IdentifierUse.OFFICIAL.equals(e.getUse())).count();
                if (systemIdCount < 1) {
                    vale.add(new ValE("should be at least one OFFICIAL type identifier")
                            .addIheRequirement("https://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier"));
                }
                long usualIdCount = listResource.getIdentifier().stream()
                        .filter(e -> e.hasUse() && Identifier.IdentifierUse.USUAL.equals(e.getUse()) && MhdTransforms.URN_IETF_RFC_3986.equals(e.getSystem())).count();
                if (usualIdCount < 1) {
                    vale.add(new ValE("1) Expecting an OID (URI) according to ITI TF Vol 3:4.2.3.3.12 SubmissionSet.uniqueId. " +
                            "2) MHD v4.0.1: If the value is a full URI, then the system SHALL be "+ MhdTransforms.URN_IETF_RFC_3986 +".")
                    .addIheRequirement("https://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier:uniqueId.value"));
                } else {
                    Optional<Identifier> usualIdentifier = IdBuilder.getUsualTypeIdentifier(listResource);
                    if (usualIdentifier.isPresent()) {
                       String idValue = Utils.stripUrnPrefixes(usualIdentifier.get().getValue());
                       mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, idValue,
                               mhdTransforms.getrMgr().allocateSymbolicId(),
                               wrapper.getAssignedId(),
                               "XDSSubmissionSet.uniqueId", idBuilder);
                       wrapper.setAssignedUid(Utils.stripUrnPrefixes(idValue));
                   } else {
                       vale.add(new ValE("Unexpected error finding an USUAL type Identifier"));
                   }
                }
            }
        }


        if (listResource.hasExtension(getAll().get(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION))) {
           Extension extension = listResource.getExtensionByUrl(getAll().get(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION));
           if (extension.getValue() instanceof Identifier) {
               Identifier identifier = (Identifier)extension.getValue();
               mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefixes(identifier.getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId(), "XDSSubmissionSet.sourceId", null);
           }
        }

        if (listResource.hasSubject() && listResource.getSubject().hasReference()) {
            mhdTransforms.addSubject(ss, wrapper, new Ref(listResource.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
        } else if (CanonicalUriCodeEnum.MINIMAL.equals(getDetectedBundleProfile())) {
            mhdTransforms.linkDummyPatient(wrapper, vale, channelConfig, assigningAuthorities, ss);
        }

        return ss;
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

