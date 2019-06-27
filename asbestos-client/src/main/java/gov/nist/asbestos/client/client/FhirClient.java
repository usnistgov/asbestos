package gov.nist.asbestos.client.client;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FhirClient {
    private Format format = Format.JSON;
    private ResourceCacheMgr resourceCacheMgr = null;

    private ResourceWrapper readResource(Ref ref, Format format) {
        HttpGet getter = new HttpGet();
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setUrl(ref);
        wrapper.setGetter(getter);
        String contentType = (format == Format.XML) ? "application/fhir+xml" : "application/fhir+json";
            getter.get(ref.getUri(), contentType);
        String resourceText = getter.getResponseText();
        if (resourceText == null)
            return wrapper;
        IBaseResource resource;
        if (format == Format.XML)
            resource = ProxyBase.getFhirContext().newXmlParser().parseResource(resourceText);
        else
            resource = ProxyBase.getFhirContext().newJsonParser().parseResource(resourceText);
        wrapper.setResource(resource);
        return wrapper;
    }

    public ResourceWrapper readResource(Ref uri) {
        Objects.requireNonNull(uri);
        Optional<ResourceWrapper> cached = readCachedResource(uri);
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
            IBaseResource resource = comp.getResource();
            ResourceWrapper wrapper1 = new ResourceWrapper();
            wrapper1.setResource(resource);
            wrapper1.setUrl(new Ref(fullUrl));
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
}
