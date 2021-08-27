package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.FhirClientBuilder;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ListResource;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * V3.x specific implementation
 */
public class MhdV3x implements MhdProfileVersionInterface {
    static String comprehensiveMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle";
    static String minimalMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle";
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(DocumentManifest.class, DocumentReference.class, Binary.class, ListResource.class);
    private static List<String> profiles = Arrays.asList(comprehensiveMetadataProfile, minimalMetadataProfile);
    private static MhdVersionEnum mhdVersionEnum = MhdVersionEnum.MHDv3x;
    private Val val;
    Boolean isMinimalMetadata = null;
    /**
     * Only used to count number of documentManifests in the Bundle.
     */
    DocumentManifest documentManifest = null;
    MhdTransforms mhdTransforms;

    public MhdV3x(Val val, MhdTransforms mhdTransforms) {
        this.val = val;
        this.mhdTransforms = mhdTransforms;
    }

    @Override
    public MhdVersionEnum getMhdVersionEnum() {
        return mhdVersionEnum;
    }

    @Override
    public Boolean isMinimalMetadata() throws Exception {
        if (isMinimalMetadata == null) {
            throw new Exception("isMinimalMetadata is not initialized.");
        }
        return isMinimalMetadata;
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
     * Checks bundle profile canonical URI and sets the property isMinimalMetadata
     * @param bundle
     */
    @Override
    public void evalBundleProfile(Bundle bundle) {
        if (bundle.getMeta().getProfile().size() != 1)
            val.add(new ValE("No profile declaration present in bundle").asError()
                    .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
        try {
            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            if (!profiles.contains(bundleProfile.asStringValue()))
                val.add(new ValE("Do not understand profile declared in bundle - " + bundleProfile).asError()
                        .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
            if (bundleProfile.asStringValue().equals(minimalMetadataProfile))
                isMinimalMetadata = new Boolean(true);
        } catch (Exception e) {
            val.add(new ValE("Bundle.meta.profile missing").asError()
                    .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
        }
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

    private RegistryPackageType createSubmissionSet(IdBuilder idBuilder, ResourceWrapper resource, ValE vale, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
        DocumentManifest dm = (DocumentManifest) resource.getResource();

        if (dm.hasIdentifier()) {
            if (dm.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("DocumentManifest has Identifier (entryUUID)").asError());
        }

        RegistryPackageType ss = new RegistryPackageType();

        ValE tr;
        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + resource.getAssignedId() + ")"));
        ss.setId(resource.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (dm.hasCreated())
            mhdTransforms.addSlot(ss, "submissionTime", mhdTransforms.translateDateTime(dm.getCreated()));
        if (dm.hasDescription())
            mhdTransforms.addName(ss, dm.getDescription());
        mhdTransforms.addClassification(ss, "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd", mhdTransforms.getrMgr().allocateSymbolicId(), resource.getAssignedId());
        if (dm.hasType())
            mhdTransforms.addClassificationFromCodeableConcept(ss, dm.getType(), CodeTranslator.CONTENTTYPECODE, resource.getAssignedId(), vale, codeTranslator);
        if (!dm.hasMasterIdentifier())
            val.add(new ValE("DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]").asError());
        else {
            mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.uniqueId", idBuilder);
            resource.setAssignedUid(Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()));
        }
        if (dm.hasSource())
            mhdTransforms.addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()), mhdTransforms.getrMgr().allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.sourceId", null);
        if (dm.hasSubject() && dm.getSubject().hasReference())
            mhdTransforms.addSubject(ss, resource,  new Ref(dm.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
        else if (isMinimalMetadata) {
            // Patient is optional in minimal metadata - add reference to No_Patient to make XDS Toolkit happy
            // Adds resource cache to configuration
            FhirClient fhirClient =
                    channelConfig == null
                            ? FhirClientBuilder.get(null)
                            : FhirClientBuilder.get(channelConfig.asChannelId());

            Optional<ResourceWrapper> patient = fhirClient.readCachedResource(new Ref("Patient/No_Patient"));
            if (patient.isPresent()) {
                ResourceWrapper thePatient = patient.get();
                Bundle patientBundle;
                if (thePatient.getResource() instanceof Bundle) {
                    patientBundle = (Bundle) thePatient.getResource();
                    Ref patRef = new Ref(patientBundle.getEntry().get(0).getFullUrl());  // this must be turned into fullURL (not relative)
                    mhdTransforms.addSubject(ss, resource, patRef , CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
                } else {
                    val.add(new ValE("Internal error - Lookup of Patient/No_Patient returned " + thePatient.getResource().getClass().getSimpleName() + " instead of Bundle").asError());
                }
            } else {
                val.add(new ValE("Internal error - cannot locate Patient/No_Patient").asError());
            }
        }
        return ss;
    }

}
