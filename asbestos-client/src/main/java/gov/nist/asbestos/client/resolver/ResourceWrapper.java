package gov.nist.asbestos.client.resolver;


import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;


import java.util.*;

public class ResourceWrapper {
    private BaseResource resource = null;    // basic content of the resource
    private String assignedId = null;         // assigned symbolic id - used in XDS submission
    private String assignedUid = null;
    private Ref ref = null;               // FHIR URL - used when available - read from server
    private HttpBase httpBase = null;        // used in operation
    private ResourceWrapper source = null;  // if httpBase is HttpPost then source  may be HttpGet that obtained resource
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    private Map<Ref, ResourceWrapper> contained = new HashMap<>();


    public ResourceWrapper(BaseResource resource) {
        this.resource = resource;
    }

    public ResourceWrapper(IBaseResource iBaseResource) {
        if (iBaseResource instanceof BaseResource)
            resource = (BaseResource) iBaseResource;
        else
            throw new Error("Cannot convert IBaseResource " + iBaseResource + " to BaseResource");
    }

    public ResourceWrapper() {

    }

    public ResourceWrapper(BaseResource resource, Ref ref) {
        this.resource = resource;
        this.ref = ref;
    }

    public ResourceWrapper(Ref ref) {
        this.ref = ref;
    }

    public String logLink() {
        if (httpBase == null)
            return null;
        return httpBase.getResponseHeaders().getHeaderValue("x-proxy-event");
    }

    public void setResource(BaseResource resource) {
        this.resource = resource;
    }

    public ResourceWrapper relativeTo(ResourceWrapper reference) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(reference.getRef());
        Objects.requireNonNull(ref);
        Ref theEnd = ref.rebase(reference.getRef());
        return new ResourceWrapper(theEnd);
    }

    public ResourceWrapper setAssignedId(String id) {
        assignedId = id;
        return this;
    }

    public ResourceWrapper setRef(Ref ref) {
        this.ref = ref;
        return this;
    }

    public String getId() {
        if (ref.getId() != null) return ref.getId();
        if (assignedId != null) return assignedId;
        throw new RuntimeException("Cannot retreive id for " + resource);
    }

    private boolean addContainedResource(ResourceWrapper resource, Val val) {
        Ref id = new Ref(resource.getId());
        boolean duplicate = contained.containsKey(id);
        if (!duplicate) contained.put(id, resource);
        if (duplicate)
            val.add(new ValE("Contained resource " + id + " is a duplicate within " + resource.getId()).asError());
        return duplicate;
    }

    public boolean isLoaded() {
        if (ref == null) return false;
        return resource != null;
    }

    public boolean isLoadable() {
        if (ref == null) return false;
        return resource == null;
    }

    public String getLocation() {
        if (httpBase != null && httpBase instanceof HttpPost) {
            HttpPost post = (HttpPost) httpBase;
            Header header = post.getLocationHeader();
            if (header != null)
                return header.getValue();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new  StringBuilder();

        String name;
        if (resource == null)
            name = "null";
        else
            name = resource.getClass().getSimpleName();

        buf.append(ref).append("(").append(name).append(")");

//        buf.append("RW[" + assignedId + ", " + ref + "] => " + name);

        return buf.toString();
    }

    public BaseResource getResource() {
        return resource;
    }

    public Map<Ref, ResourceWrapper> getContained() {
        return contained;
    }

    public Ref getRef() {
        return ref;
    }

    public String getAssignedId() {
        return assignedId;
    }

    public HttpBase getHttpBase() {
        return httpBase;
    }

    public boolean hasHttpBase() {
        return httpBase != null;
    }

    public void setHttpBase(HttpBase httpBase) {
        this.httpBase = httpBase;
    }

    public boolean isOk() {
        if (httpBase != null) {
            if ((httpBase instanceof HttpGet) || (httpBase instanceof HttpDelete)) {
                if (ref != null && httpBase != null && httpBase.getResponse() != null) {
                    String resourceType = ref.getResourceType();
                    if (!resourceType.equals("")) {
                        BaseResource resource = getResponseResource();
                        if (resource == null)
                            return false;
                        if (ref.isQuery()) {
                            if (!resource.getClass().getSimpleName().equals("Bundle"))
                                return false;
                        } else if (!resource.getClass().getSimpleName().equals(resourceType))
                            return false;
                    }
                }
            } else if (httpBase instanceof HttpPost) {
                BaseResource resource = getResponseResource();
                if (resource instanceof OperationOutcome) {
                    List<String> errors =  errorsFromOperationOutcome();
                    if (!errors.isEmpty())
                        return false;
                }
            }
        }
        int status = getStatus();
        return 200 <= status && status < 300;
    }

    public BaseResource getResponseResource() {
        if (httpBase != null && httpBase.getResponse() != null) {
            byte[] responseBytes = httpBase.getResponse();
            if (responseBytes.length == 0)
                return null;
            String contentType = httpBase.getResponseContentType();
            Format format = Format.fromContentType(contentType);
            return ProxyBase.parse(responseBytes, format);
        }
        return null;
    }

    public List<String> errorsFromOperationOutcome() {
        List<String> errors = new ArrayList<>();

        BaseResource resource = getResponseResource();
        if (resource != null && (resource instanceof OperationOutcome)) {
            OperationOutcome oo = (OperationOutcome) resource;
            for (OperationOutcome.OperationOutcomeIssueComponent component : oo.getIssue()) {
                if (component.getSeverity() == OperationOutcome.IssueSeverity.ERROR)
                    errors.add(component.getDiagnostics());
            }
        }

        return errors;
    }

    public int getStatus() {
        if (httpBase == null) {
            if (resource != null)
                return 200;  // may have come from cache
            return -1;
        }
        return httpBase.getStatus();
    }

    public boolean hasResource() {
        return resource != null;
    }

    public String getAssignedUid() {
        return assignedUid;
    }

    public void setAssignedUid(String assignedUid) {
        this.assignedUid = assignedUid;
    }
}
