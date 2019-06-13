package gov.nist.asbestos.mhd.client;

import gov.nist.asbestos.asbestosProxySupport.Base.Base;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.mhd.resolver.Ref;
import gov.nist.asbestos.mhd.resolver.ResourceCacheMgr;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FhirClient {
    private Format format = Format.JSON;
    private ResourceCacheMgr resourceCacheMgr = null;

    private Optional<ResourceWrapper> readResource(Ref ref, Format format) {
        HttpGet getter = new HttpGet();
        String contentType = (format == Format.XML) ? "application/fhir+xml" : "application/fhir+json";
            getter.get(ref.getUri(), contentType);
        String resourceText = getter.getResponseText();
        if (resourceText == null)
            return Optional.empty();
        IBaseResource resource;
        if (format == Format.XML)
            resource = Base.getFhirContext().newXmlParser().parseResource(resourceText);
        else
            resource = Base.getFhirContext().newJsonParser().parseResource(resourceText);
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setUrl(ref);
        wrapper.setResource(resource);
        return Optional.of(wrapper);
    }

    public Optional<ResourceWrapper> readResource(Ref uri) {
        Optional<ResourceWrapper> cached = readCachedResource(uri);
        if (cached.isPresent())
            return cached;
        return readResource(uri, format);
    }

    public Optional<ResourceWrapper> readCachedResource(Ref ref) {
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
        Optional<ResourceWrapper> wrapper = readResource(new Ref(query));
        if (!wrapper.isPresent())
            return new ArrayList<>();
        assert wrapper.get().getResource() instanceof Bundle;
        Bundle bundle = (Bundle) wrapper.get().getResource();

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

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
    }
}
