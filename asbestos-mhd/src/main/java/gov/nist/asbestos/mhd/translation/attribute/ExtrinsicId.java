package gov.nist.asbestos.mhd.translation.attribute;

import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;
import java.util.Objects;

public class ExtrinsicId extends AbstractAttribute {
    private ValE val = null;
    private Identifier identifier = null;
    private ResourceMgr rMgr = null;


    public String getId(List<Identifier> identifierList) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);
        ValE tr = val.addTr(new ValE("Identifier to entryUUID"));
        String retVal = null;
        if (identifierList != null) {
            for (Identifier id : identifierList) {
                if (id.hasValue()) {
                    boolean isOfficial = id.hasUse() && id.getUse() == Identifier.IdentifierUse.OFFICIAL;
                    if (ResourceMgr.isUUID(id.getValue())) {
                        if (!isOfficial)
                            tr.add(new ValE("DocumentReference.identifier is UUID but not labeled as official").asWarning());
                        else {
                            tr.add(new ValE("Official Identifier found").asTranslation());
//                            resource.setAssignedId(id.getValue());
                            retVal = id.getValue();
                            break;
                        }
                    } else {
                        if (isOfficial)
                            tr.add(new ValE("Identifier is labeled Official but is not UUID format").asError());
                    }
                }
            }
        }
        if (retVal == null)
            retVal = rMgr.allocateSymbolicId();
        return retVal;
    }

    public Identifier getIdentifier(String id) {
        Objects.requireNonNull(val);
        ValE tr = val.addTr(new ValE("entryUUID to Identifier"));
        identifier = new Identifier();
        identifier.setSystem("urn:ietf:rfc:3986");
        identifier.setValue(id);
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

    public ExtrinsicId setrMgr(ResourceMgr rMgr) {
        this.rMgr = rMgr;
        return this;
    }

    @Override
    public ExtrinsicId setVal(ValE val) {
        this.val = val;
        return this;
    }

}
