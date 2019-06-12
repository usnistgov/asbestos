package gov.nist.asbestos.mhd.translation;

import ca.uhn.fhir.model.api.IValueSetEnumBinder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.mhd.exceptions.MetadataAttributeTranslationException;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ExtrinsicObjectToDocumentReference implements IVal {
    private Val val;
    private CodeTranslator codeTranslator;

    public DocumentReference getDocumentReference(ExtrinsicObjectType eo) {
        DocumentReference dr = new DocumentReference();

        String objectType = eo.getObjectType();
        if (!"urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1".equals(objectType)) {
            val.add(new ValE("ExtrinsicObjectToDocumentReference: this transform only handles stable DocumentEntries - objectType " + objectType + " received").asError());
            return dr;
        }

        if (eo.getId() != null) {
            Identifier idr = new Identifier();
            idr.setSystem("urn:ietf:rfc:3986");
            idr.setValue(stripUrnPrefix(eo.getId()));
            dr.getIdentifier().add(idr);
        }
        for (ExternalIdentifierType ei : eo.getExternalIdentifier()) {
            String scheme = ei.getIdentificationScheme();
            if ("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427".equals(scheme)) {
                // PatientID
                PatientId patientId = new PatientId().setPatientid(ei.getValue());
                String system = "urn:oid:" + patientId.getAa();
                String id = patientId.getId();
                List<String> searchParams = new ArrayList<>();
                searchParams.add("identifier=" + system + "|" + id);

            } else if ("urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab".equals(scheme)) {
                // Unique ID
                Identifier idr = new Identifier();
                idr.setSystem("urn:ietf:rfc:3986");
                idr.setValue(stripUrnPrefix(ei.getValue()));
                dr.setMasterIdentifier(idr);
            } else {
                val.add(new ValE("ExtrinsicObjectToDocumentReference: Do not understand ExternalIdentifier identification scheme " + scheme).asError());
                return dr;
            }
        }
        dr.addContent(new DocumentReference.DocumentReferenceContentComponent());
        Attachment attachment = new Attachment();
        dr.getContent().get(0).setAttachment(attachment);
        attachment.setContentType(eo.getMimeType());
        DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
        dr.setContext(context);
        for (SlotType1 slot : eo.getSlot()) {
            String name = slot.getName();
            List<String> values = slot.getValueList().getValue();
            if (!values.isEmpty()) {
                String value1 = values.get(0);
                if ("hash".equals(name)) {
                    byte[] value;
                    attachment.setHash(value);
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

        if (eo.getStatus().endsWith("Approved")) {
            dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        } else if (eo.getStatus().endsWith("Deprecated")) {
            dr.setStatus(Enumerations.DocumentReferenceStatus.SUPERSEDED);
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
            InternationalStringType ist = eo.getDescription();
            List<LocalizedStringType> local = ist.getLocalizedString();
            if (!local.isEmpty()) {
                LocalizedStringType lst = local.get(0);
                String value = lst.getValue();
                attachment.setTitle(value);
            }
        }

        return dr;
    }

    private static String stripUrnPrefix(String id) {
        if (id == null) return id;
        if (id.startsWith("urn:uuid:")) return id.substring("urn:uuid:".length());
        if (id.startsWith("urn:oid:")) return id.substring("urn:oid:".length());
        return id;
    }
    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
