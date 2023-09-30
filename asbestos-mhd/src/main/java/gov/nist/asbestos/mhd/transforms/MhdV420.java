package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdCanonicalUriCodeInterface;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.mhd.channel.MhdIgInterface;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static gov.nist.asbestos.mhd.transforms.MhdV410Common.SUBMISSION_SET_PROFILE_DOCREF_SUFFIX;

public class MhdV420 implements MhdIgInterface {
    private final String BUNDLE_RESOURCES_DOC_REF = String.format("3.65.4.1.2.1 Bundle Resources. %s",  getDocBase("ITI-65.html#23654121-bundle-resources"));
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(ListResource.class, DocumentReference.class, Binary.class);
    private static MhdIgImplEnum mhdVersionEnum = MhdIgImplEnum.MHDv420;

    private static final Logger logger = Logger.getLogger(MhdV420.class.getName());

    public MhdV420() {
    }


    @Override
    public MhdIgImplEnum getMhdIgImpl() {
        return mhdVersionEnum;
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
     *
     * Builds a submission set only if FHIR List resource matches a submission set type.
     * Returns null if resource is not a submission set type.
     * @param mhdTransforms
     * @param wrapper
     * @param val
     * @param vale
     * @param idBuilder
     * @param channelConfig
     * @param codeTranslator
     * @param assigningAuthorities
     * @param canonicalUriCodeEnum
     * @return
     */
    @Override
    public RegistryPackageType buildSubmissionSet(MhdTransforms mhdTransforms, ResourceWrapper wrapper, Val val, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities, CanonicalUriCodeEnum canonicalUriCodeEnum) {

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

//        return new MhdV410Common(this, mhdTransforms, canonicalUriCodeEnum).buildSubmissionSet( wrapper, val, vale, idBuilder, channelConfig, codeTranslator, assigningAuthorities);
        try {
            BaseResource resource = wrapper.getResource();
            vale.setMsg("The Document Recipient shall transform the Bundle content into a proper message for the Given grouped Actor. " +
                    "(Document Recipient is grouped with XDS Document Source. " +
                    "Transformation task: Transforming List to SubmissionSet.)" +
                    getDocBase("ITI-65.html#23654131-grouping-with-actors-in-other-document-sharing-profiles"));

//            Class<? extends MhdCanonicalUriCodeInterface> myCodesClass = mhdImpl.getMhdVersion().getUriCodesClass();
//            if (myCodesClass != null) {
//                MhdCanonicalUriCodeInterface codesImpl = myCodesClass.getDeclaredConstructor().newInstance();
            if (MhdCanonicalUriCodeInterface.isCodedAsAListType(Arrays.asList(getMhdIgImpl()), resource, CanonicalUriCodeEnum.SUBMISSIONSET)) {
                return createSubmissionSet(mhdTransforms, canonicalUriCodeEnum, idBuilder, wrapper, val, vale, channelConfig, codeTranslator, assigningAuthorities);
            }
//            }
        } catch (Exception ex) {
            logger.warning(ex.toString());
        }

        return null;

    }


    private RegistryPackageType createSubmissionSet(MhdTransforms mhdTransforms, CanonicalUriCodeEnum canonicalUriCodeEnum, IdBuilder idBuilder, ResourceWrapper wrapper, Val val, ValE vale, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        ListResource listResource = (ListResource)wrapper.getResource();

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("SubmissionSet of ListResource type has Identifier (entryUUID)")
                        .asDoc()
                        .addIheRequirement("2:3.65.4.1.2 Message Semantics: " +
                                "SubmissionSet of ListResource type Identifer has an optional entryUUID."
                                + getDocBase("ITI-65.html#2365412-message-semantics")));
        }

        RegistryPackageType ss = new RegistryPackageType();

        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + wrapper.getAssignedId() + ")"));
        ss.setId(wrapper.getAssignedId());
        ss.setObjectType(MhdV410Common.SUBMISSION_SET_XDS_OBJECT_TYPE);

        if (listResource.hasMode()) {
            if (! ListResource.ListMode.WORKING.equals(listResource.getMode())) {
                vale.add(new ValE("Mode Required Pattern: working")
                        .asError()
                        .addIheRequirement(getDocBase(SUBMISSION_SET_PROFILE_DOCREF_SUFFIX)));
            }
        } else {
            vale.add(new ValE("Mode is required [1..1]")
                    .asError()
                    .addIheRequirement(getDocBase(SUBMISSION_SET_PROFILE_DOCREF_SUFFIX)));
        }

        if (listResource.hasDate())
            mhdTransforms.addSlot(ss, "submissionTime", mhdTransforms.translateDateTime(listResource.getDate()));
        if (listResource.hasTitle()) {
            mhdTransforms.addName(ss, listResource.getTitle());
        }
        // 3bdd: Submission set type classification
        mhdTransforms.addClassification(ss, MhdTransforms.URN_UUID__BDD_SUBMISSION_SET, mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId());

        MhdCanonicalUriCodeInterface myCodesImpl = getUriCodesClass();

        if (listResource.hasExtension(myCodesImpl.getUriCodeMap().get(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL))) {
            Extension extension = listResource.getExtensionByUrl(myCodesImpl.getUriCodeMap().get(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL));
            if (extension.getValue() instanceof CodeableConcept) {
                CodeableConcept codeableConcept = (CodeableConcept) extension.getValue();
                mhdTransforms.addClassificationFromCodeableConcept(ss, codeableConcept, CodeTranslator.CONTENTTYPECODE, wrapper.getAssignedId(), vale, codeTranslator);
            }
        }

        if (listResource.hasIdentifier()) {
            if (listResource.getIdentifier().size() < 1) {
                vale.add(new ValE("Minimum List identifier cardinality is less than 1 (IG bug because it does not satisfy the XDS actor optionality table requirements?).*")
                        .asError()
                        .addIheRequirement(getDocBase( "ITI-65.html#2365412-message-semantics")));
            }
            long usualIdCount = listResource.getIdentifier().stream()
                    .filter(e -> e.hasUse() && Identifier.IdentifierUse.USUAL.equals(e.getUse()) && MhdTransforms.URN_IETF_RFC_3986.equals(e.getSystem())).count();
            if (usualIdCount < 1) {
                vale.add(new ValE("1) Expecting an OID (URI) according to ITI TF Vol 3:4.2.3.3.12 SubmissionSet.uniqueId. " +
                        "2) MHD StructureDefinition: If the value is a full URI, then the system SHALL be "+ MhdTransforms.URN_IETF_RFC_3986 +".")
                        .asError()
                        .addIheRequirement(getDocBase( "StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier:uniqueId.value")));
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
                    vale.add(new ValE("Unexpected error finding an USUAL type Identifier")
                            .asError()
                            .addIheRequirement(getDocBase( "StructureDefinition-IHE.MHD.Minimal.SubmissionSet-definitions.html#List.identifier")));
                }
            }
        }


        if (listResource.hasExtension(myCodesImpl.getUriCodeMap().get(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION))) {
            Extension extension = listResource.getExtensionByUrl(myCodesImpl.getUriCodeMap().get(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION));
            if (extension.getValue() instanceof Identifier) {
                Identifier identifier = (Identifier)extension.getValue();
                mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefixes(identifier.getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId(), "XDSSubmissionSet.sourceId", null);
            }
        }

        if (listResource.hasSubject() && listResource.getSubject().hasReference()) {
            mhdTransforms.addSubject(ss, wrapper, new Ref(listResource.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
        } else if (CanonicalUriCodeEnum.MINIMAL.equals(canonicalUriCodeEnum)) {
            mhdTransforms.linkDummyPatient(wrapper, vale, channelConfig, assigningAuthorities, ss);
        }

        return ss;
    }




    @Override
    public String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers) {
        return rMgr.allocateSymbolicId();
    }



}
