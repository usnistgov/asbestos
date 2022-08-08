package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.exceptions.MetadataAttributeTranslationException;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.translation.attribute.DateTransform;
import gov.nist.asbestos.mhd.translation.attribute.PatientId;
import gov.nist.asbestos.mhd.translation.attribute.Slot;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Reference;

import java.util.List;
import java.util.Optional;

public class SubmissionSetToListResource {
    private Val val = null;
    private CodeTranslator codeTranslator = null;
    private ResourceCacheMgr resourceCacheMgr = null;
    private ContainedIdAllocator containedIdAllocator = null;
    private FhirClient fhirClient = null;
    private MhdProfileVersionInterface mhdImpl;

    public SubmissionSetToListResource(MhdProfileVersionInterface mhdImpl, ContainedIdAllocator containedIdAllocator, ResourceCacheMgr resourceCacheMgr, CodeTranslator codeTranslator, FhirClient fhirClient, Val val) {
        this.val = val;
        this.codeTranslator = codeTranslator;
        this.resourceCacheMgr = resourceCacheMgr;
        this.containedIdAllocator = containedIdAllocator;
        this.fhirClient = fhirClient;
        this.mhdImpl = mhdImpl;
    }

    public ListResource getListResource(RegistryPackageType ss, List<AssociationType1> assocs, ChannelConfig channelConfig) {
        ListResource listResource = new ListResource();

        String id = null;
        if (ss.getId() != null) {
            id = ss.getId();
            Identifier idr = new Identifier();
            idr.setSystem(MhdTransforms.URN_IETF_RFC_3986);
            idr.setValue(ss.getId());
            idr.setUse(Identifier.IdentifierUse.OFFICIAL);
            listResource.getIdentifier().add(idr);
        }
        for (ExternalIdentifierType ei : ss.getExternalIdentifier()) {
            String scheme = ei.getIdentificationScheme();
            if ("urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446".equals(scheme)) {
                // Patient ID
                PatientId patientId = new PatientId()
                        .setPatientid(ei.getValue())
                        .setResourceCacheMgr(resourceCacheMgr)
                        .setFhirClient(fhirClient);
                // TEST-1000-XXX is No_Patient - fake patient used when Minimal Metadata has not Patient reference
                if (!"TEST-1000-XXX".equals(patientId.getId())) {
                    patientId.setVal(val);
                    Optional<Reference> reference = patientId.getFhirReference();
                    reference.ifPresent(listResource::setSubject);
                }
            } else if ("urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8".equals(scheme)) {
                // Derive logicalId from the USUAL id to overcome differentiation problem with a GET request
                String logicalId = IdBuilder.makeOpaqueLogicalId(IdBuilder.SS_OPAQUE_ID, Utils.stripUrnPrefixes(ei.getValue()));
                listResource.setId(logicalId);

                // Unique ID
                Identifier idr = new Identifier();
                idr.setSystem("urn:ietf:rfc:3986");
                idr.setValue(Utils.addUrnOidPrefix(ei.getValue()));
                idr.setUse(Identifier.IdentifierUse.USUAL);
                listResource.getIdentifier().add(idr);
            } else if ("urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832".equals(scheme)) {
                // source ID
                Identifier idr = new Identifier().setValue(Utils.addUrnOidPrefix(ei.getValue()));
                listResource.addExtension(MhdV4Constants.iheSourceIdExtensionUrl, idr); // one required [1..1]
            } else {
                val.add(new ValE("SubmissionSetToListResource: Do not understand ExternalIdentifier identification scheme " + scheme).asError());
            }
        }
        for (ClassificationType c : ss.getClassification()) {
            String scheme = c.getClassificationScheme();
            if ("urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d".equals(scheme)) {
                // author - not implemented yet
            } else if ("urn:uuid:aa543740-bdda-424e-8c96-df4873be8500".equals(scheme)) {
                // content type
                XdsCode xdsCode = new XdsCode()
                        .setCodeTranslator(codeTranslator)
                        .setClassificationType(c);
                xdsCode.setVal(val);
                listResource.addExtension(MhdV4Constants.iheDesignationTypeExtensionUrl, xdsCode.asCodeableConcept()); // max is one [0..1]
            }
        }

        // List type is SubmissionSet
        {
            CodeableConcept listCode = new CodeableConcept(new Coding(MhdV4Constants.ssListTypeCodeSystem, MhdV4Constants.ssListTypeCodeValue, null));
            listResource.setCode(listCode);
        }

        if (ss.getName() != null)
            listResource.setTitle(Slot.getValue(ss.getName()));

        if (ss.getStatus().endsWith("Approved"))
            listResource.setMode(ListResource.ListMode.WORKING);

        List<SlotType1> slots = ss.getSlot();
        for (SlotType1 slot : slots) {
            if ("submissionTime".equals(slot.getName())) {
                try {
                    listResource.setDate(DateTransform.dtmToDate(slot.getValueList().getValue().get(0)));
                } catch (MetadataAttributeTranslationException e) {
                    val.err("Error translating SubmissionSet.submissionTime");
                }
            }
        }


        for (AssociationType1 assoc : assocs) {
            if (assoc.getAssociationType().endsWith("HasMember") && assoc.getSourceObject().equals(id)) {
//                String fhirBase = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) + "/proxy/" + channelConfig.asFullId();
                String reference = channelConfig.getProxyURI() +
                        "/DocumentReference/" + // TODO: Folder support
                        Utils.stripUrnPrefixes(assoc.getTargetObject());
                listResource.getEntry().add(new ListResource.ListEntryComponent(new Reference(reference)));
            }
        }

        return listResource;
    }
}
