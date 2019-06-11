package gov.nist.asbestos.mhd.transactionSupport;


import gov.nist.asbestos.mhd.resolver.Ref;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.instance.model.api.IBaseResource;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResourceWrapper {
    private IBaseResource resource = null;    // basic content of the resource
    private String assignedId = null;         // assigned symbolic id - used in XDS submissionm
    private Ref url = null;               // FHIR URL - used when available - read from server
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    private Map<Ref, ResourceWrapper> contained = new HashMap<>();


    public ResourceWrapper(IBaseResource resource) {
        this.resource = resource;
    }

    public ResourceWrapper() {

    }

    public ResourceWrapper(IBaseResource resource, Ref url) {
        this.resource = resource;
        this.url = url;
    }

    public ResourceWrapper(Ref url) {
        this.url = url;
    }

    public void setResource(IBaseResource resource) {
        this.resource = resource;
    }

    public ResourceWrapper relativeTo(ResourceWrapper reference) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(reference.getUrl());
        Objects.requireNonNull(url);
        Ref theEnd = url.rebase(reference.getUrl());
        return new ResourceWrapper(theEnd);
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

    public boolean isLoaded() {
        if (url == null) return false;
        return resource != null;
    }

    public boolean isLoadable() {
        if (url == null) return false;
        return resource == null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new  StringBuilder();

        String name;
        if (resource == null)
            name = "null";
        else
            name = resource.getClass().getSimpleName();

        buf.append(url).append("(").append(name).append(")");

//        buf.append("RW[" + assignedId + ", " + url + "] => " + name);

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
