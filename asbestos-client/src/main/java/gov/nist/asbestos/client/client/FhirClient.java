package gov.nist.asbestos.client.client;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.events.ProxyEvent;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.PatientCacheMgr;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.net.URI;
import java.util.*;

public class FhirClient {
    private Format format = Format.JSON;
    private ResourceCacheMgr resourceCacheMgr = null;
    private HttpBase httpBase = null;
    private Op op = null;
    private PatientCacheMgr patientCacheMgr = null;
    private boolean requestGzip = false;
    private boolean sendGzip = false;
    private Header supportRequestHeader = new Header("x-ftk-support", "true");
    private boolean supportRequest = false;

    public FhirClient() {}

    public FhirClient requestGzip(boolean requestGzip) {
        this.requestGzip = requestGzip;
        return this;
    }

    public FhirClient sendGzip(boolean sendGzip) {
        this.sendGzip = sendGzip;
        return this;
    }

    public ResourceWrapper writeResource(BaseResource resource, Ref ref, Format format, Headers headers) {
        return writeResource(resource, ref, format, headers.asMap());
    }

    public ResourceWrapper writeResource(BaseResource resource, Ref ref, Format format, Map<String, String> headers) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(ref);
        ResourceWrapper response = new ResourceWrapper();
        if (headers == null)
            headers = new HashMap<>();
        if (format == null)
            format = Format.XML;
        String contentType = format.getContentType(); //(format == Format.XML) ? "application/fhir+xml" : (format == Format.JSON ? "application/fhir+json" : null);
        if (contentType != null)
            headers.put("content-type", contentType);
        Headers theHeaders = new Headers(headers);
        if (isSupportRequest())
            theHeaders.add(supportRequestHeader);
        if (theHeaders.getHeaderValue("accept") == null)
            theHeaders.add(new Header("accept", contentType));

        HttpPost post = new HttpPost();
        if (sendGzip)
            post.sendGzip();
        if (requestGzip)
            post.acceptGzip();
        post.setRequestHeaders(theHeaders);
        post.setUri(ref.getUri());
        byte[] content = null;
        if (resource != null) {
            if (format == Format.JSON)
                content = ProxyBase.getFhirContext().newJsonParser().encodeResourceToString(resource).getBytes();
            else
                content = ProxyBase.getFhirContext().newXmlParser().encodeResourceToString(resource).getBytes();
        }

        post.setRequest(content);

        post.post();

        if (post.getLocationHeader() != null)
            response.setRef(new Ref(post.getLocationHeader().getValue()));
        boolean isXml = post.getResponseContentType().contains("xml");
        String returnedResourceText = post.getResponseText();
        if (returnedResourceText != null && !returnedResourceText.equals("")) {
            if (!returnedResourceText.startsWith("{") && !returnedResourceText.startsWith("<"))
                throw new RuntimeException(returnedResourceText);
            IBaseResource returnedResource;
            try {
                returnedResource = isXml
                        ? ProxyBase.getFhirContext().newXmlParser().parseResource(returnedResourceText)
                        : ProxyBase.getFhirContext().newJsonParser().parseResource(returnedResourceText);
                if (returnedResource instanceof BaseResource)
                    response.setResource((BaseResource) returnedResource);
            } catch (Exception e) {
                //
            }
        }
        response.setHttpBase(post);
        this.httpBase = post;
        op = Op.POST;

        return response;
    }

    public ResourceWrapper deleteResource(Ref ref, Map<String, String> headers) {
        Objects.requireNonNull(ref);
        ResourceWrapper response = new ResourceWrapper();
        if (headers == null)
            headers = new HashMap<>();
        HttpDelete delete = new HttpDelete();
        Headers theHeaders = new Headers(headers);
        if (isSupportRequest())
            theHeaders.add(supportRequestHeader);
        delete.setRequestHeaders(theHeaders);
        delete.setUri(ref.getUri());

        delete.run();

        response.setHttpBase(delete);
        this.httpBase = delete;
        op = Op.DELETE;

        return response;
    }

    public ResourceWrapper writeResource(ResourceWrapper resource, Ref ref, Format format, Map<String, String> headers) {
        return writeResource(resource.getResource(), ref, format, headers);
    }


    public ResourceWrapper readResource(Ref ref, Format format) {
        HttpGet getter = new HttpGet();
        if (requestGzip)
            getter.acceptGzip();
        httpBase = getter;
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setRef(ref);
        wrapper.setHttpBase(getter);
        String contentType = (format == Format.JSON) ? "application/fhir+json" : "application/fhir+xml";
        Headers theHeaders = new Headers();
        if (isSupportRequest()) {
            theHeaders.add(supportRequestHeader);
            getter.setRequestHeaders(theHeaders);
        }
        getter.get(ref.getUri(), contentType);
        return gobbleGetResponse(getter, wrapper, format);
    }

    Format asFormat(Headers headers) {
        String contentType = headers.getContentType().getValue();
        if ("application/fhir+xml".equals(contentType))
            return  Format.XML;
        if ("application/fhir+json".equals(contentType))
            return Format.JSON;
        return Format.NONE;
    }

    Format asResponseFormat(Headers headers) {
        String contentType = headers.getAccept().getValue();
        if ("application/fhir+xml".equals(contentType))
            return  Format.XML;
        if ("application/fhir+json".equals(contentType))
            return Format.JSON;
        return Format.NONE;
    }

    private ResourceWrapper gobbleGetResponse(HttpGet getter, ResourceWrapper wrapper, Format format) {
        if (getter.getStatus() != 200) {
            return wrapper;
        }

        String resourceText = getter.getResponseText();
        if (resourceText == null || resourceText.equals("")) {
            if (getter.isSearch())
                throw new Error("Search must return Bundle - received nothing instead");
            return wrapper;
        }
        BaseResource resource = null;
        IBaseResource iBaseResource;
        if (format == Format.JSON) {
            iBaseResource = ProxyBase.getFhirContext().newJsonParser().parseResource(resourceText);
            if (iBaseResource instanceof BaseResource)
                resource = (BaseResource) iBaseResource;
        } else {
            iBaseResource = ProxyBase.getFhirContext().newXmlParser().parseResource(resourceText);
            if (iBaseResource instanceof BaseResource)
                resource = (BaseResource) iBaseResource;
        }
//        if (getter.isSearch()) {
//            if (!(resource instanceof Bundle)) {
//                throw new Error("Search must return Bundle - received " + resource.getClass().getSimpleName() + " instead");
//            }
//        } else {
            Ref ref = new Ref(getter.getUri());
            String expectedResourceType = ref.getResourceType();
            String returnedResourceType = resource.getClass().getSimpleName();
            if (expectedResourceType != null && !expectedResourceType.equals("")) {
                if (!returnedResourceType.equals("OperationOutcome")) {
                    if (!expectedResourceType.equals("metadata") && !returnedResourceType.equals("CapabilityStatement")) {
                        if (!returnedResourceType.equals(expectedResourceType)) {
                            if (getter.isSearch() && returnedResourceType.equals("Bundle")) {
                                Bundle bundle = (Bundle) resource;
                                if (bundle.hasEntry()) {
                                    if (!bundle.getEntry().get(0).getResource().getClass().getSimpleName().equals(expectedResourceType))
                                        throw new Error("Search returned bundle containing " + bundle.getEntry().get(0).getResource().getClass().getSimpleName() + " instead of " + expectedResourceType);
                                }
                            } else
                                throw new Error("Read must return " + expectedResourceType + " - received " + resource.getClass().getSimpleName() + " instead");
                        }
                    }
                }
            }
//        }
        wrapper.setResource(resource);
        this.httpBase = getter;
        op = Op.GET;

        return wrapper;
    }

    public ResourceWrapper readResource(Ref ref, Map<String, String> requestHeader) {
        if ("Patient".equals(ref.getResourceType()) && patientCacheMgr != null) {
            PatientCacheMgr.PatientCacheItem item = patientCacheMgr.find(ref);
            if (item != null)
                return item.getWrapper();
        }
        HttpGet getter = new HttpGet();
        if (requestGzip)
            getter.acceptGzip();
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setRef(ref);
        wrapper.setHttpBase(getter);
        getter.setUri(ref.getUri());
        Headers headers = new Headers(requestHeader);
        if (isSupportRequest())
            headers.add(supportRequestHeader);
        String contentType = format == null ? Format.XML.getContentType() : format.getContentType();
        if (headers.getHeaderValue("accept") == null)
            headers.add(new Header("accept", contentType));
        getter.setRequestHeaders(headers);
        getter.get();
        ResourceWrapper theWrapper = gobbleGetResponse(getter, wrapper, asResponseFormat(headers));
        if (theWrapper.hasResource() && theWrapper.getResource() instanceof Patient && patientCacheMgr != null) {
            patientCacheMgr.add(theWrapper);
        }
        if (theWrapper.hasRef() && theWrapper.getRef().hasId() && theWrapper.hasResource() && theWrapper.getResource().getId() == null) {
            theWrapper.getResource().setId(theWrapper.getRef().getId());
        }
        return theWrapper;
    }

    public ResourceWrapper readResource(Ref ref) {
        Objects.requireNonNull(ref);
        Optional<ResourceWrapper> cached = readCachedResource(ref);
        if (cached.isPresent())
            return cached.get();
        if ("".equals(ref.getBase().asString()))
            return new ResourceWrapper(ref);

        if ("Patient".equals(ref.getResourceType()) && patientCacheMgr != null) {
            PatientCacheMgr.PatientCacheItem item = patientCacheMgr.find(ref);
            if (item != null)
                return item.getWrapper();
        }

        ResourceWrapper theWrapper = cached.orElseGet(() -> readResource(ref, getFormat()));
        if (theWrapper.hasResource() && theWrapper.getResource() instanceof Patient && patientCacheMgr != null) {
            patientCacheMgr.add(theWrapper);
        }
        return theWrapper;
    }

    public Optional<ResourceWrapper> readCachedResource(Ref ref) {
        Objects.requireNonNull(ref);
        if (resourceCacheMgr != null) {
            ResourceWrapper fromCache = resourceCacheMgr.getResource(ref);
            if (fromCache != null)
                return Optional.of(fromCache);
        }
        return Optional.empty();
    }

    public List<ResourceWrapper> search(Ref base, Class<?> resourceType, List<String> params, boolean stopAtFirst, boolean lookInCache) {
        List<ResourceWrapper> list = new ArrayList<>();

        if (lookInCache) {
            List<ResourceWrapper> cached = searchCache(base, resourceType, params, stopAtFirst);
            list.addAll(cached);
            if (stopAtFirst && !list.isEmpty())
                return list;
        }

        URI query = QueryBuilder.buildUrl(base, resourceType, params);
        ResourceWrapper wrapper = readResource(new Ref(query));
        if (wrapper.getResource() == null)
            return new ArrayList<>();
        assert wrapper.getResource() instanceof Bundle;
        Bundle bundle = (Bundle) wrapper.getResource();

        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            String fullUrl = comp.getFullUrl();
            Resource resource = comp.getResource();
            ResourceWrapper wrapper1 = new ResourceWrapper();
            wrapper1.setResource(resource);
            wrapper1.setRef(new Ref(fullUrl));
            list.add(wrapper1);
        }

        return list;
    }

    public List<ResourceWrapper> searchCache(Ref base, Class<?> resourceType, List<String> params, boolean stopAtFirst) {
        if (resourceCacheMgr == null)
            return new ArrayList<>();
        return resourceCacheMgr.search(base, resourceType, params, stopAtFirst);
    }

    public List<Ref> getCachedServers() {
        if (resourceCacheMgr == null)
            return new ArrayList<>();
        return resourceCacheMgr.getCachedServers();
    }

    public FhirClient setFormat(Format format) {
        this.format = format;
        return this;
    }

    public Format getFormat() {
        return format;
    }

    public FhirClient setResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
        resourceCacheMgr.setFhirClient(this);
        return this;
    }

    public ResourceCacheMgr getResourceCacheMgr() {
        return resourceCacheMgr;
    }

    public HttpBase getHttpBase() {
        return httpBase;
    }

    public Op getOp() {
        return op;
    }

    public int getStatus() {
        if (httpBase == null)
            return -1;
        return httpBase.getStatus();
    }

    public void setPatientCacheMgr(PatientCacheMgr patientCacheMgr) {
        this.patientCacheMgr = patientCacheMgr;
    }

    public UIEvent getProxyEvent() {
        return new ProxyEvent(httpBase).getEvent();
    }

    public boolean isSupportRequest() {
        return supportRequest;
    }

    public FhirClient setSupportRequest(boolean supportRequest) {
        this.supportRequest = supportRequest;
        return this;
    }
}
