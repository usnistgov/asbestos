package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.mhd.exceptions.MetadataAttributeTranslationException;
import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.r4.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// TODO author, related, binary (content.attachment), logical id, identifier (entryUUID), legalAuthenticator, sourcePatientInfo, sourcePatientId
public class DocumentEntryToDocumentReference implements IVal {
    private Val val;
    private CodeTranslator codeTranslator;
    private ResourceMgr resourceMgr = null;

    public DocumentReference getDocumentReference(ExtrinsicObjectType eo) {
        Objects.requireNonNull(eo);
        DocumentReference dr = new DocumentReference();

        String objectType = eo.getObjectType();
        if (!"urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1".equals(objectType)) {
            val.add(new ValE("DocumentEntryToDocumentReference: this transform only handles stable DocumentEntries - objectType " + objectType + " received").asError());
            return dr;
        }

        DocumentReference.DocumentReferenceContentComponent content = new DocumentReference.DocumentReferenceContentComponent();
        Attachment attachment = new Attachment();
        dr.addContent(content);
        attachment.setContentType(eo.getMimeType());
        dr.getContent().get(0).setAttachment(attachment);
        DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
        dr.setContext(context);

        if (eo.getId() != null) {
            String id = eo.getId();
            Identifier idr = new Identifier();
            idr.setSystem("urn:ietf:rfc:3986");
            idr.setValue(stripUrnPrefix(id));
            if (ResourceMgr.isUUID(id))
                idr.setUse(Identifier.IdentifierUse.OFFICIAL);
            dr.getIdentifier().add(idr);
        }
        for (ExternalIdentifierType ei : eo.getExternalIdentifier()) {
            String scheme = ei.getIdentificationScheme();
            if ("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427".equals(scheme)) {
                // PatientID
                PatientId patientId = new PatientId()
                        .setPatientid(ei.getValue())
                        .setResourceMgr(resourceMgr);
                patientId.setVal(val);
                Optional<Reference> reference = patientId.getFhirReference();
                reference.ifPresent(dr::setSubject);
            } else if ("urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab".equals(scheme)) {
                // Unique ID
                Identifier idr = new Identifier();
                idr.setSystem("urn:ietf:rfc:3986");
                idr.setValue(stripUrnPrefix(ei.getValue()));
                dr.setMasterIdentifier(idr);
            } else {
                val.add(new ValE("DocumentEntryToDocumentReference: Do not understand ExternalIdentifier identification scheme " + scheme).asError());
            }
        }
        for (ClassificationType c : eo.getClassification()) {
            String scheme = c.getClassificationScheme();
            if ("urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d".equals(scheme)) {
                // Author
            } else {
                XdsCode xdsCode = new XdsCode()
                        .setCodeTranslator(codeTranslator)
                        .setClassificationType(c);
                xdsCode.setVal(val);
                xdsCode.setCodeTranslator(codeTranslator);

                if (CodeTranslator.FORMATCODE.equals(scheme)) {
                    content.setFormat(xdsCode.asCoding());
                } else if (CodeTranslator.CLASSCODE.equals(scheme)) {
                    dr.setCategory(Collections.singletonList(xdsCode.asCodeableConcept()));
                } else if (CodeTranslator.PRACCODE.equals(scheme)) {
                    context.setPracticeSetting(xdsCode.asCodeableConcept());
                } else if (CodeTranslator.HCFTCODE.equals(scheme)) {
                    context.setFacilityType(xdsCode.asCodeableConcept());
                } else if (CodeTranslator.EVENTCODE.equals(scheme)) {
                    context.setEvent(Collections.singletonList(xdsCode.asCodeableConcept()));
                } else if (CodeTranslator.CONFCODE.equals(scheme)) {
                    dr.setSecurityLabel(Collections.singletonList(xdsCode.asCodeableConcept()));
                } else if (CodeTranslator.TYPECODE.equals(scheme)) {
                    dr.setType(xdsCode.asCodeableConcept());
                } else {
                    val.add(new ValE("DocumentEntryToDocumentReference: Do not understand Classification scheme " + scheme).asError());
                }
            }
        }
        for (SlotType1 slot : eo.getSlot()) {
            String name = slot.getName();
            List<String> values = slot.getValueList().getValue();
            if (!values.isEmpty()) {
                String value1 = values.get(0);
                if ("hash".equals(name)) {
                    attachment.setHash(HashTranslator.toByteArray(value1));
                } else if ("size".equals(name)) {
                    attachment.setSize(Integer.parseInt(value1));
                } else if ("repositoryUniqueId".equals(name)) {

                } else if ("languageCode".equals(name)) {
                    attachment.setLanguage(value1);
                } else if ("serviceStartTime".equals(name)) {
                    if (!context.hasPeriod())
                        context.setPeriod(new Period());
                    Period period = context.getPeriod();
                    try {
                        period.setStart(DateTransform.dtmToDate(value1));
                    } catch (MetadataAttributeTranslationException e) {
                        val.add(new ValE(e.getMessage()).asError());
                    }
                } else if ("serviceStopTime".equals(name)) {
                    if (!context.hasPeriod())
                        context.setPeriod(new Period());
                    Period period = context.getPeriod();
                    try {
                        period.setEnd(DateTransform.dtmToDate(value1));
                    } catch (MetadataAttributeTranslationException e) {
                        val.add(new ValE(e.getMessage()).asError());
                    }
                } else if ("creationTime".equals(name)) {
                    try {
                        dr.setDate(DateTransform.dtmToDate(value1));
                    } catch (MetadataAttributeTranslationException e) {
                        val.add(new ValE(e.getMessage()).asError());
                    }
                } else if ("sourcePatientId".equals(name)) {

                } else if ("sourcePatientInfo".equals(name)) {

                } else if ("legalAuthenticator".equals(name)) {

                } else if ("referenceIdList".equals(name)) {

                }
            }
        }

        if (eo.getStatus() != null) {
            if (eo.getStatus().endsWith("Approved")) {
                dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
            } else if (eo.getStatus().endsWith("Deprecated")) {
                dr.setStatus(Enumerations.DocumentReferenceStatus.SUPERSEDED);
            }
        }

        if (eo.getName() != null) {
            InternationalStringType ist = eo.getName();
            List<LocalizedStringType> local = ist.getLocalizedString();
            if (!local.isEmpty()) {
                LocalizedStringType lst = local.get(0);
                String value = lst.getValue();
                dr.setDescription(value);
            }
        }

        if (eo.getDescription() != null) {
            String desc = Slot.getValue(eo.getDescription());
            if (desc != null)
                attachment.setTitle(desc);
        }

        return dr;
    }

    public static String stripUrnPrefix(String id) {
        if (id == null) return id;
        if (id.startsWith("urn:uuid:")) return id.substring("urn:uuid:".length());
        if (id.startsWith("urn:oid:")) return id.substring("urn:oid:".length());
        return id;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }

    public void setResourceMgr(ResourceMgr resourceMgr) {
        this.resourceMgr = resourceMgr;
    }

    public void setCodeTranslator(CodeTranslator codeTranslator) {
        this.codeTranslator = codeTranslator;
    }
}
