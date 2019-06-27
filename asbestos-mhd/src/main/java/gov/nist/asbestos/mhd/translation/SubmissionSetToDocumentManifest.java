package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.r4.model.*;

import java.util.Optional;


// TODO author
// TODO how to translate sourceID
public class SubmissionSetToDocumentManifest implements IVal {
    private Val val = null;
    private CodeTranslator codeTranslator = null;
    private ResourceMgr resourceMgr = null;

    public DocumentManifest getDocumentManifest(RegistryPackageType ss) {
        DocumentManifest dm = new DocumentManifest();

        if (ss.getId() != null) {
            Identifier idr = new Identifier();
            idr.setSystem("urn:ietf:rfc:3986");
            idr.setValue(stripUrnPrefix(ss.getId()));
            dm.getIdentifier().add(idr);
        }
        for (ExternalIdentifierType ei : ss.getExternalIdentifier()) {
            String scheme = ei.getIdentificationScheme();
            if ("urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446".equals(scheme)) {
                // Patient ID
                PatientId patientId = new PatientId()
                        .setPatientid(ei.getValue())
                        .setResourceMgr(resourceMgr);
                patientId.setVal(val);
                Optional<Reference> reference = patientId.getFhirReference();
                reference.ifPresent(dm::setSubject);
            } else if ("urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8".equals(scheme)) {
                // Unique ID
                Identifier idr = new Identifier();
                idr.setSystem("urn:ietf:rfc:3986");
                idr.setValue(stripUrnPrefix(ei.getValue()));
                dm.setMasterIdentifier(idr);
            } else if ("urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832".equals(scheme)) {
                // source ID
            } else {
                val.add(new ValE("SubmissionSetToDocumentManifest: Do not understand ExternalIdentifier identification scheme " + scheme).asError());
            }
        }
        for (ClassificationType c : ss.getClassification()) {
            String scheme = c.getClassificationScheme();
            if ("urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d".equals(scheme)) {
                // author
            } else if ("urn:uuid:aa543740-bdda-424e-8c96-df4873be8500".equals(scheme)) {
                // content type
                XdsCode xdsCode = new XdsCode()
                        .setCodeTranslator(codeTranslator)
                        .setClassificationType(c);
                xdsCode.setVal(val);
                dm.setType(xdsCode.asCodeableConcept());
            } else {
                val.add(new ValE("SubmissionSetToDocumentManifest: Do not understand Classification scheme " + scheme).asError());
            }
        }
        if (ss.getName() != null)
            dm.setDescription(Slot.getValue(ss.getName()));
        if (ss.getStatus().endsWith("Approved"))
            dm.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        return dm;
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

    public void setCodeTranslator(CodeTranslator codeTranslator) {
        this.codeTranslator = codeTranslator;
    }

    public void setResourceMgr(ResourceMgr resourceMgr) {
        this.resourceMgr = resourceMgr;
    }
}
