package gov.nist.asbestos.mhd.resolver;

import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * load by a factory - either TestResourceCacheFactory or ResourceCacheMgrFactory
 *
 * Manages multiple resource caches.  Each cache is identified by its BaseUrl
 *
 */
public class ResourceCacheMgr {
    private static final Logger logger = Logger.getLogger(ResourceCacheMgr.class);
    private Map<Ref, ResourceCache> caches = new HashMap<>();  // baseUrl -> cache
    private File externalCache;

    public ResourceCacheMgr(File externalCache) {
        this.externalCache = externalCache;
        if (externalCache != null)
            loadCache(new File(externalCache, "fhirResourceCache"));
    }

    public void loadCache(File cacheCollectionDir) {
        File[] files = cacheCollectionDir.listFiles();
        if (files != null) {
            for (File cache : files) {
                if (cache.isDirectory() && new File(cache, "cache.properties").exists()) {
                    logger.info("Scanning Resource Cache directory " + cache);
                    FileSystemResourceCache rcache = new FileSystemResourceCache(cache);
                    caches.put(rcache.base, rcache);
                }
            }
        }
    }

    public void add(Ref uri, IBaseResource resource) {
        if (!uri.isAbsolute())
            throw new RuntimeException("ResourceCacheMgr#add: cannot load " + uri + " - must be absolute URI");
        Ref fhirbase = uri.getBase();
        ResourceCache cache = caches.get(fhirbase);
        if (cache == null) {
            cache = new MemoryResourceCache();
            caches.put(fhirbase, cache);
        }
        cache.add(uri.getRelative(), new ResourceWrapper(resource));
    }


    /**
     * return resource or throw exception
     * @param fullUrl
     * @return
     */
     ResourceWrapper getResource(Ref fullUrl) {
        assert fullUrl.isAbsolute();
        Ref baseUrl = fullUrl.getBase();
        ResourceCache cache = caches.get(baseUrl);
        if (cache == null)
            throw new RuntimeException("Cannot access " + fullUrl + "\nNo cache defined for baseUrl " + baseUrl + "\nCaches exist for " + caches.keySet());
        return cache.readResource(fullUrl.getRelative());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName() + "\n");
        caches.forEach((Ref key, ResourceCache value) -> {
            buf.append(key).append(" => ").append(value).append("\n");
        });

        return buf.toString();
    }

}
