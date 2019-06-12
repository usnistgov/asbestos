package gov.nist.asbestos.mhd.resolver;

import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * load by a factory - either TestResourceCacheFactory or ResourceCacheMgrFactory
 *
 * Manages multiple resource caches.  Each cache is identified by its BaseUrl
 *
 */
public class ResourceCacheMgr {
    private static final Logger logger = Logger.getLogger(ResourceCacheMgr.class);
    private Map<Ref, ResourceCache> caches = new HashMap<>();  // baseUrl -> cache

    public ResourceCacheMgr(File externalCache) {
        Objects.requireNonNull(externalCache);
        assert externalCache.isDirectory();
        loadCache(new File(externalCache, "resourceCache"));
    }

    private void loadCache(File cacheCollection) {
        if (cacheCollection.exists() && cacheCollection.isDirectory()) {
            File[] dirs = cacheCollection.listFiles();
            if (dirs == null)
                return;
            for (File dir : dirs) {
                if (dir.isDirectory() && new File(dir, "cache.properties").exists()) {
                    logger.info("Scanning Resource Cache directory " + dir);
                    FileSystemResourceCache rcache = new FileSystemResourceCache(dir);
                    caches.put(rcache.getBase(), rcache);
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

    public ResourceWrapper getResource(Ref fullUrl) {
        if (fullUrl.isAbsolute()) {
            Ref baseUrl = fullUrl.getBase();
            ResourceCache cache = caches.get(baseUrl);
            if (cache == null)
                return null;
            return cache.readResource(fullUrl.getRelative());
        }
        return null;
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
