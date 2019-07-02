package gov.nist.asbestos.client.client;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpPost;
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

    public ResourceWrapper writeResource(BaseResource resource, Ref ref, Format format, Map<String, String> headers) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(ref);
        ResourceWrapper response = new ResourceWrapper();
        if (headers == null)
            headers = new HashMap<>();
        if (format == null)
            format = Format.XML;
        String contentType = (format == Format.XML) ? "application/fhir+xml" : (format == Format.JSON ? "application/fhir+json" : null);
        if (contentType != null)
            headers.put("content-type", contentType);
        HttpPost post = new HttpPost();
        post.setRequestHeaders(new Headers(headers));
        post.setUri(ref.getUri());
        byte[] content;
        if (format == Format.JSON)
            content = ProxyBase.getFhirContext().newJsonParser().encodeResourceToString(resource).getBytes();
        else
            content = ProxyBase.getFhirContext().newXmlParser().encodeResourceToString(resource).getBytes();
        post.setRequest(content);

        post.post();

        response.setRef(ref);
        response.setResource(resource);
        response.setHttpBase(post);
        this.httpBase = post;
        op = Op.POST;

        return response;
    }

    public ResourceWrapper writeResource(ResourceWrapper resource, Ref ref, Format format, Map<String, String> headers) {
        return writeResource(resource.getResource(), ref, format, headers);
    }


    private ResourceWrapper readResource(Ref ref, Format format) {
        HttpGet getter = new HttpGet();
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setRef(ref);
        wrapper.setHttpBase(getter);
        String contentType = (format == Format.JSON) ? "application/fhir+json" : "application/fhir+xml";
        getter.get(ref.getUri(), contentType);
        return gobbleGetResponse(getter, wrapper, format);
    }

    public ResourceWrapper readResource(Ref ref, Map<String, String> requestHeader) {
        HttpGet getter = new HttpGet();
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setRef(ref);
        wrapper.setHttpBase(getter);
        getter.setUri(ref.getUri());
        Headers headers = new Headers(requestHeader);
        getter.setRequestHeaders(headers);
        getter.get();
        return gobbleGetResponse(getter, wrapper, asFormat(headers));
    }

    Format asFormat(Headers headers) {
        String contentType = headers.getContentType().getValue();
        if ("application/fhir+xml".equals(contentType))
            return  Format.XML;
        if ("application/fhir+json".equals(contentType))
            return Format.JSON;
        return Format.NONE;
    }

    private ResourceWrapper gobbleGetResponse(HttpGet getter, ResourceWrapper wrapper, Format format) {
        String resourceText = getter.getResponseText();
        if (resourceText == null)
            return wrapper;
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
        wrapper.setResource(resource);
        this.httpBase = getter;
        op = Op.GET;

        return wrapper;
    }

    public ResourceWrapper readResource(Ref uri) {
        Objects.requireNonNull(uri);
        Optional<ResourceWrapper> cached = readCachedResource(uri);
        if (cached.isPresent())
            return cached.get();
        if ("".equals(uri.getBase().asString()))
            return new ResourceWrapper(uri);
        return cached.orElseGet(() -> readResource(uri, format));
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

    public FhirClient setResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
        return this;
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
}
