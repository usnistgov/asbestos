package gov.nist.asbestos.mhd;

import org.hl7.fhir.r4.model.BaseResource;

public class SubmittedObject {
    private String uid;
    private BaseResource resource;

    public SubmittedObject(String uid, BaseResource resource) {
        this.uid = uid;
        this.resource = resource;
    }

    public String getUid() {
        return uid;
    }

    public BaseResource getResource() {
        return resource;
    }
}
