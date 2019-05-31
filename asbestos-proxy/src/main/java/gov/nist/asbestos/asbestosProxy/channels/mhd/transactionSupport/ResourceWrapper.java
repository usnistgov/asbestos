package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;


import gov.nist.asbestos.asbestosProxy.channels.mhd.resolver.Ref;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.instance.model.api.IBaseResource;


import java.util.HashMap;
import java.util.Map;

public class ResourceWrapper {
    private IBaseResource resource;    // basic content of the resource
    private String assignedId;         // assigned symbolic id - used in XDS submissionm
    private Ref url;               // FHIR URL - used when available - read from server
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    private Map<Ref, ResourceWrapper> contained = new HashMap<>();


    public ResourceWrapper(IBaseResource resource) {
        this.resource = resource;
    }

    public ResourceWrapper(IBaseResource resource, Ref url) {
        this.resource = resource;
        this.url = url;
    }

    public ResourceWrapper setId(String id) {
        assignedId = id;
        return this;
    }

    public ResourceWrapper setUrl(Ref url) {
        this.url = url;
        return this;
    }

    public String getId() {
        if (url.getId() != null) return url.getId();
        if (assignedId != null) return assignedId;
        throw new RuntimeException("Cannot retreive id for " + resource);
    }

    private boolean addContainedResource(ResourceWrapper resource, Val val) {
        Ref id = new Ref(resource.getId());
        boolean duplicate = contained.containsKey(id);
        if (!duplicate) contained.put(id, resource);
        if (duplicate)
            val.err(new Val().msg("Contained resource ${id} is a duplicate within ${resource.id}"));
        return duplicate;
    }

    @Override
    public String toString() {
        StringBuilder buf = new  StringBuilder();

        buf.append("RW[" + assignedId + ", " + url + "] => " + resource.getClass().getSimpleName());

        return buf.toString();
    }

    public IBaseResource getResource() {
        return resource;
    }

    public Map<Ref, ResourceWrapper> getContained() {
        return contained;
    }

    public Ref getUrl() {
        return url;
    }

    public String getAssignedId() {
        return assignedId;
    }
}
