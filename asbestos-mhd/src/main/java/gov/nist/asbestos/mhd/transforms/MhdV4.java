package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mappings for XDS and MHD Mapping (XDS)
 * SubmissionSet
 * List	XDS SubmissionSet
 *    meta
 *       profile	SubmissionSet.limitedMetadata
 *    extension (designationType)	SubmissionSet.contentTypeCode
 *    extension (sourceId)	SubmissionSet.sourceId
 *    (IGNORED MAPPING) extension (intendedRecipient)	SubmissionSet.intendedRecipient
 *    identifier	SubmissionSet.entryUUID and SubmissionSet.uniqueId
 *    status	SubmissionSet.availabilityStatus
 *    IGNORED ?? mode	shall be 'working'
 *    title	SubmissionSet.title
 *    code	shall be 'submissionset'
 *    subject	SubmissionSet.patientId
 *    date	SubmissionSet.submissionTime
 *    source	SubmissionSet.author
 *       extension (authorOrg)	SubmissionSet.author when the author is an Organization
 *    (IGNORED) note	SubmissionSet.comments
 *    (IGNORED?) entry
 *       item	references to DocumentReference(s) and Folder List(s)
 *       (Is this verified by the DocumentSource tests PDBRequestMinEval.xml ?)
 */
public class MhdV4 implements MhdProfileVersionInterface {
    static String comprehensiveMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle";
    static String minimalMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle";
    static String iheDesignationTypeExtensionUrl = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType";
    static String iheSourceIdExtensionUrl = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId";
    /**
     * TODO
     */
    static String containedMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.UnContained.Comprehensive.ProvideBundle"; // TODO
    private static Map<String, String> listTypeMap  =
        Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>("submissionset", "http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
    private static List<String> profiles = Arrays.asList(comprehensiveMetadataProfile, minimalMetadataProfile);
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(ListResource.class, DocumentReference.class, Binary.class);
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv4;

    private Val val;
    private MhdTransforms mhdTransforms;
    Boolean isMinimalMetadata = null;
    ListResource submissionSetListResource;


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
        if (resource instanceof ListResource) {
            vale.setMsg("The Document Recipient shall transform the Bundle content into a proper message for the Given grouped Actor. " +
                    "(Document Recipient is grouped with XDS Document Source. " +
                    "Transformation task: Transforming List to SubmissionSet.)" +
                    "https://profiles.ihe.net/ITI/MHD/ITI-65.html#23654131-grouping-with-actors-in-other-document-sharing-profiles");

            ListResource listResource = (ListResource)resource;
            String code = "submissionset";
            String system = listTypeMap.get(code);
            if (listResource.getCode().hasCoding(system, code)) {
                if (listResource.getCode().getCoding().stream().filter(e -> system.equals(e.getSystem()) && code.equals(e.getCode())).count() == 1) {
                    vale.setMsg("Found ListResource with code " + code);
                    return createSubmissionSet(idBuilder, wrapper, vale, channelConfig, codeTranslator, assigningAuthorities);
                } else {
                    vale.add(new ValE("ListResource has multiple codes of " + code).asWarning());
                }
            }
        }
        return null;

    }

    private RegistryPackageType createSubmissionSet(IdBuilder idBuilder, ResourceWrapper wrapper, ValE vale, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        ListResource listResource = (ListResource)wrapper.getResource();

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("DocumentManifest has Identifier (entryUUID)").asError()
                        .addIheRequirement("2:3.65.4.1.2 Message Semantics: " +
                                "The Document Source shall not provide any entryUUID values."
                        + "See https://profiles.ihe.net/ITI/MHD/ITI-65.html#2365412-message-semantics"));
        }

        RegistryPackageType ss = new RegistryPackageType();

        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + wrapper.getAssignedId() + ")"));
        ss.setId(wrapper.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (listResource.hasDate())
            mhdTransforms.addSlot(ss, "submissionTime", mhdTransforms.translateDateTime(listResource.getDate()));
        if (listResource.hasTitle()) {
            mhdTransforms.addName(ss, listResource.getTitle());
        }
        // 3bdd: Submission set type classification
        mhdTransforms.addClassification(ss, "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd", mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId());

        if (listResource.hasExtension(iheDesignationTypeExtensionUrl)) {
           Extension extension = listResource.getExtensionByUrl(iheDesignationTypeExtensionUrl);
           if (extension.getValue() instanceof CodeableConcept) {
              CodeableConcept codeableConcept = (CodeableConcept)extension.getValue();
               mhdTransforms.addClassificationFromCodeableConcept(ss, codeableConcept, CodeTranslator.CONTENTTYPECODE, wrapper.getAssignedId(), vale, codeTranslator);
           }
        }

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().size() < 2) {
               vale.add(new ValE("Minimum List identifier cardinality is less than 2. Should be 2..*")
                       .addIheRequirement("https://profiles.ihe.net/ITI/MHD/StructureDefinition-IHE.MHD.Minimal.SubmissionSet.html"));
            } else {
                long systemIdCount = listResource.getIdentifier().stream()
                        .filter(e -> e.hasUse() && Identifier.IdentifierUse.OFFICIAL.equals(e.getUse())).count();
                if (systemIdCount < 1) {
                    vale.add(new ValE("should be at least one OFFICIAL type identifier")
                            .addIheRequirement("https://profiles.ihe.net/ITI/MHD/StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier"));
                }
                long usualIdCount = listResource.getIdentifier().stream()
                        .filter(e -> e.hasUse() && Identifier.IdentifierUse.USUAL.equals(e.getUse())).count();
                if (usualIdCount < 1) {
                    vale.add(new ValE("should be at least one USUAL type identifier")
                    .addIheRequirement("https://profiles.ihe.net/ITI/MHD/StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier"));
                } else {
                    Optional<Identifier> usualIdentifier = listResource.getIdentifier().stream()
                            .filter(e -> e.hasUse() && Identifier.IdentifierUse.USUAL.equals(e.getUse()))
                            .findFirst();
                   if (usualIdentifier.isPresent()) {
                       String idValue = Utils.stripUrnPrefix(usualIdentifier.get().getValue());
                       mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, idValue,
                               mhdTransforms.getrMgr().allocateSymbolicId(),
                               wrapper.getAssignedId(),
                               "XDSSubmissionSet.uniqueId", idBuilder);
                       wrapper.setAssignedUid(Utils.stripUrnPrefix(idValue));
                   }
                }
            }
        }


        if (listResource.hasExtension(iheSourceIdExtensionUrl)) {
           Extension extension = listResource.getExtensionByUrl(iheSourceIdExtensionUrl);
           if (extension.getValue() instanceof Identifier) {
               Identifier identifier = (Identifier)extension.getValue();
               mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefix(identifier.getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId(), "XDSSubmissionSet.sourceId", null);
           }
        }

        if (listResource.hasSubject() && listResource.getSubject().hasReference()) {
            mhdTransforms.addSubject(ss, wrapper, new Ref(listResource.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
        } else if (isMinimalMetadata) {
            mhdTransforms.linkDummyPatient(wrapper, vale, channelConfig, assigningAuthorities, ss);
        }

        return ss;
    }
}

