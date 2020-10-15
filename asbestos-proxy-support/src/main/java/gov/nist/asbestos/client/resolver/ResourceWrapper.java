package gov.nist.asbestos.client.resolver;


import com.google.common.base.Strings;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;


import java.io.File;
import java.net.URISyntaxException;
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
    private Bundle context = null;
    private File file = null;
    private UIEvent event = null;

    public ResourceWrapper setRequest(boolean request) {
        isRequest = request;
        return this;
    }

    private boolean isRequest;  // did content come from event "request" or "response"
    private String focusUrl; // if resource is Bundle then this can be the fullUrl of a resource in the Bundle


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

    public ResourceWrapper newWithContext() {
        ResourceWrapper newWrapper = new ResourceWrapper();
        newWrapper.ref = ref.copy();
        newWrapper.context = context;
        newWrapper.file = file;
        newWrapper.event = event;
        newWrapper.isRequest = isRequest;
        return newWrapper;
    }

    public String logLink() {
        if (httpBase == null)
            return null;
        return httpBase.getResponseHeaders().getHeaderValue("x-proxy-event");
    }

    public ResourceWrapper setEvent(UIEvent event, boolean isRequest) {
        this.event = event;
        this.isRequest = isRequest;
        if (event != null && ref == null)
            this.setRef(new Ref(event.getURI()));
        return this;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public UIEvent getEvent() {
        return event;
    }

    public boolean hasEvent() {
        return getEvent() != null;
    }

    public ResourceWrapper setResource(BaseResource resource) {
        this.resource = resource;
        return this;
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

    public ResourceWrapper setContext(Bundle context) {
        this.context = context;
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

    private String[] getXProxyEventPathParts() {
        if (httpBase == null)
            return null;
        Headers responseHeaders = httpBase.getResponseHeaders();
        if (responseHeaders == null)
            return null;
        String eventUrl = responseHeaders.getHeaderValue("x-proxy-event");
        if (eventUrl == null)
            return null;
        String[] parts = eventUrl.split("/");
        if (parts.length <= 0)
            return null;
        return parts;
    }

    public String getEventId() {
        String[] parts = getXProxyEventPathParts();
        if (parts == null)
            return null;
        if (parts.length <= 0)
            return null;
        return parts[parts.length - 1];
    }

    public String getResponseResourceType() {
        String[] parts = getXProxyEventPathParts();
        if (parts == null)
            return null;
        if (parts.length < 2)
            return null;
        return parts[parts.length - 2];
    }

    public String getResourceType() {
        BaseResource thisResource = getResource();
        if (thisResource == null)
            return null;
        return thisResource.getClass().getSimpleName();
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
        if (resource != null)
            return resource;
        if (httpBase != null && httpBase instanceof HttpGetter) {
            HttpGetter getter = (HttpGetter) httpBase;
            if (getter.getStatus() == 404)
                return null;
            if (getter.getResponseText() != null) {
                resource = ParserBase.parse(getter.getResponse(), Format.fromContentType(getter.getResponseContentType()));
                return resource;
            }
        }
        if (hasEvent()) {
            String body = isRequest ? getEvent().getRequestBody() : getEvent().getResponseBody();
            if (Strings.isNullOrEmpty(body))
                return null;
            resource = ParserBase.parse(body, Format.fromContent(body));
            if (resource instanceof Bundle) {
                Bundle bundle = (Bundle) resource;
                if (Strings.isNullOrEmpty(focusUrl)) {
                    return bundle;
                } else {
                    for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
                        if (focusUrl.equals(comp.getFullUrl())) {
                            resource = comp.getResource();
                            return resource;
                        }
                    }
                }
                HttpGetter getter = new HttpGetter();
                try {
                    getter.get(focusUrl);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(focusUrl + ": " + e.getMessage());
                }
                if (getter.isSuccess()) {
                    String gbody = getter.getResponseText();
                    resource = ParserBase.parse(gbody, Format.fromContent(body));
                    return resource;
                }
                return bundle;
            }
            return resource;
        }
        if (httpBase == null && ref != null) {
            HttpGetter getter = new HttpGetter();
            httpBase = getter;
            try {
                getter.get(ref.getUri().toString());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            if (getter.getStatus() == 404)
                return null;
            if (getter.getResponseText() != null) {
                resource = ParserBase.parse(getter.getResponse(), Format.fromContentType(getter.getResponseContentType()));
                return resource;
            }
        }
        return null;
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
            if ((httpBase instanceof HttpGetter) || (httpBase instanceof HttpDelete)) {
                if (ref != null && httpBase != null && httpBase.getResponse() != null) {
                    String resourceType = ref.getResourceType();
                    if (!resourceType.equals("")) {
                        BaseResource resource = getResponseResource();
                        if (resource == null)
                            return false;
                        if (ref.isQuery()) {
                            if (resource.getClass().getSimpleName().equals("Bundle"))
                                return true;
                            if (resource.getClass().getSimpleName().equals(resourceType))
                                return true;
                            return false;
                        } else
                            if (!resource.getClass().getSimpleName().equals(resourceType))
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
            String responseString = httpBase.getResponseText();
            String contentType = httpBase.getResponseContentType();
            Format format = Format.fromContentType(contentType);
            if (format == null)
                return null;
            return ParserBase.parse(responseString, format);
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
        if (resource != null)
            return true;
        if (httpBase != null && httpBase instanceof HttpGetter) {
            HttpGetter getter = (HttpGetter) httpBase;
            if (getter.getResponse() != null)
                return true;
        }
        return false;
    }

    public String getAssignedUid() {
        return assignedUid;
    }

    public void setAssignedUid(String assignedUid) {
        this.assignedUid = assignedUid;
    }

    public boolean hasRef() {
        return ref != null;
    }

    public File getFile() {
        return file;
    }

    public ResourceWrapper setFile(File file) {
        this.file = file;
        return this;
    }

    public String getFocusUrl() {
        return focusUrl;
    }

    public ResourceWrapper setFocusUrl(String focusUrl) {
        this.focusUrl = focusUrl;
        return this;
    }

    public boolean hasFocusUrl() {
        return focusUrl != null;
    }
}
