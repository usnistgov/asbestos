package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.channel.MhdVersionEnum;
import gov.nist.asbestos.mhd.channel.UriCodeTypeEnum;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.translation.attribute.ExtrinsicId;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;
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
 * V3.x specific implementation
 */
public class MhdV3x implements MhdProfileVersionInterface {
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(DocumentManifest.class, DocumentReference.class, Binary.class, ListResource.class /* List not fully supported in V3 mode for now*/);

    private static final String IHE_BUNDLE_RESOURCE_REFERENCE_DOCREF = "3.65.4.1.2.1 Bundle Resources";
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv3x;
    private Val val;
    CanonicalUriCodeEnum mhdBundleProfileEnum;
    /**
     * Only used to count number of documentManifests in the Bundle.
     */
    DocumentManifest documentManifest = null;
    MhdTransforms mhdTransforms;
    private static final Logger logger = Logger.getLogger(MhdV3x.class.getName());

    public MhdV3x(Bundle b, Val val, MhdTransforms mhdTransforms) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(mhdTransforms);
        this.val = val;
        this.mhdTransforms = mhdTransforms;
        try {
            this.mhdBundleProfileEnum = detectBundleProfileType(b);
        } catch (Exception ex) {
            this.mhdBundleProfileEnum = null;
            logger.warning("mhdBundleProfileEnum is null. Exception: " + ex );
        }

    }

    @Override
    public CanonicalUriCodeEnum getDetectedBundleProfile() {
        return mhdBundleProfileEnum;
    }

    @Override
    public String getIheReference() {
        return IHE_BUNDLE_RESOURCE_REFERENCE_DOCREF;
    }


    @Override
    public MhdVersionEnum getMhdVersion() {
        return mhdVersionEnum;
    }


    @Override
    public List<Class<?>> getAcceptableResourceTypes() {
        return acceptableResourceTypes;
    }


    public RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        BaseResource resource = wrapper.getResource();
        if (resource instanceof DocumentManifest) {
            DocumentManifest dm = (DocumentManifest) resource;
            if (documentManifest == null) {
                documentManifest = dm;
                return createSubmissionSet(idBuilder, wrapper, vale, channelConfig, codeTranslator, assigningAuthorities);
            } else {
                vale.add(new ValE("Found multiple DocumentManifests - one required").asError());
            }
        }
        return null;
    }

    private RegistryPackageType createSubmissionSet(IdBuilder idBuilder, ResourceWrapper wrapper, ValE vale, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        DocumentManifest dm = (DocumentManifest) wrapper.getResource();

        if (dm.hasIdentifier()) {
            if (dm.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("DocumentManifest has Identifier (entryUUID)").asError()
                .addIheRequirement("3.65.4.1.2 Message Semantics  " +
                        "The Document Source shall not provide any entryUUID values."));
        }

        RegistryPackageType ss = new RegistryPackageType();

        ValE tr;
        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + wrapper.getAssignedId() + ")"));
        ss.setId(wrapper.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (dm.hasCreated())
            mhdTransforms.addSlot(ss, "submissionTime", MhdTransforms.translateDateTime(dm.getCreated()));
        if (dm.hasDescription()) {
            // Mapped to Title according to MHD 3.1 Table 4.5.1.2-1: FHIR DocumentManifest mapping to SubmissionSet,
            // and the 4th column in table maps to Name object. See https://profiles.ihe.net/ITI/TF/Volume3/ch-4.2.html#4.2.3.3.11
            mhdTransforms.addName(ss, dm.getDescription());
        }
        mhdTransforms.addClassification(ss, MhdTransforms.URN_UUID__BDD_SUBMISSION_SET, mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId());
        if (dm.hasType())
            mhdTransforms.addClassificationFromCodeableConcept(ss, dm.getType(), CodeTranslator.CONTENTTYPECODE, wrapper.getAssignedId(), vale, codeTranslator);
        if (!dm.hasMasterIdentifier())
            val.add(new ValE("DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]. See Table 4.5.1.2-1: FHIR DocumentManifest mapping to SubmissionSet. See SubmissionSet uniqueId requirement as per the optionality table: https://profiles.ihe.net/ITI/TF/Volume3/ch-4.3.html#4.3.1").asError());
        else {
            mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, Utils.stripUrnPrefixes(dm.getMasterIdentifier().getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId(), "XDSSubmissionSet.uniqueId", idBuilder);
            wrapper.setAssignedUid(Utils.stripUrnPrefixes(dm.getMasterIdentifier().getValue()));
        }
        if (dm.hasSource())
            mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefixes(dm.getMasterIdentifier().getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), wrapper.getAssignedId(), "XDSSubmissionSet.sourceId", null);
        if (dm.hasSubject() && dm.getSubject().hasReference())
            mhdTransforms.addSubject(ss, wrapper,  new Ref(dm.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
        else if (CanonicalUriCodeEnum.MINIMAL.equals(getDetectedBundleProfile())) {
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
        return new ExtrinsicId()
                .setVal(valE)
                .setrMgr(rMgr)
                .getId(identifiers);

    }


}
