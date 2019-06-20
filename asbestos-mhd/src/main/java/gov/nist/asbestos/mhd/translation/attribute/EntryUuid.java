package gov.nist.asbestos.mhd.translation.attribute;

import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;
import java.util.Objects;

import static gov.nist.asbestos.mhd.translation.DocumentEntryToDocumentReference.stripUrnPrefix;

public class EntryUuid extends AbstractAttribute {
    private ValE val = null;
    private ResourceWrapper resource = null;
    private Identifier identifier = null;
    private ResourceMgr rMgr = null;

    public EntryUuid setResource(ResourceWrapper resource) {
        this.resource = resource;
        return this;
    }

    public ResourceWrapper assignId(List<Identifier> identifierList) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(resource);
        Objects.requireNonNull(rMgr);
        ValE tr = val.addTr(new ValE("Identifier to entryUUID"));
        if (identifierList != null) {
            for (Identifier id : identifierList) {
                if (id.hasValue() && ResourceMgr.isUUID(id.getValue())) {
                    boolean isOfficial = id.hasUse() && id.getUse() == Identifier.IdentifierUse.OFFICIAL;
                    if (!isOfficial)
                        tr.add(new ValE("DocumentReference.identifier is UUID but not labeled as official").asWarning());
                    else {
                        tr.add(new ValE("Official Identifier found").asTranslation());
                        resource.setAssignedId(id.getValue());
                    }
                }
            }
        }
        if (resource.getAssignedId() == null)
            resource.setAssignedId(rMgr.allocateSymbolicId());
        return resource;
    }

    public Identifier getIdentifier(String id) {
        Objects.requireNonNull(val);
        ValE tr = val.addTr(new ValE("entryUUID to Identifier"));
        identifier = new Identifier();
        identifier.setSystem("urn:ietf:rfc:3986");
        identifier.setValue(stripUrnPrefix(id));
        if (ResourceMgr.isUUID(id)) {
            identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
            tr.add(new ValE("Official Identifier attached"));
        } else {
            tr.add(new ValE("Non-official Identifier attached"));
        }

        return identifier;
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public EntryUuid setrMgr(ResourceMgr rMgr) {
        this.rMgr = rMgr;
        return this;
    }

    @Override
    public EntryUuid setVal(ValE val) {
        this.val = val;
        return this;
    }

}
