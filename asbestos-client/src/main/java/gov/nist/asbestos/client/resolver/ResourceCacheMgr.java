package gov.nist.asbestos.client.resolver;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;

import java.io.File;
import java.util.*;

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

    public ResourceCacheMgr(File dir, Ref baseUrl) {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(baseUrl);
        assert dir.exists() && dir.isDirectory();
        FileSystemResourceCache rcache = new FileSystemResourceCache(dir, baseUrl);
        caches.put(baseUrl, rcache);
    }

    public List<Ref> getCachedServers() {
        return new ArrayList<>(caches.keySet());
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
        Objects.requireNonNull(fullUrl);
        if (fullUrl.isAbsolute()) {
            Ref baseUrl = fullUrl.getBase();
            ResourceCache cache = caches.get(baseUrl);
            if (cache == null)
                return null;
            return cache.readResource(fullUrl.getRelative());
        } else {
            Ref baseUrl = fullUrl.getBase();
            ResourceCache cache = caches.get(baseUrl);
            if (cache == null)
                return null;
            return cache.readResource(fullUrl.getRelative());
        }
        //return null;
    }

    public List<ResourceWrapper> search(Ref base, Class<?> resourceType, List<String> params, boolean stopAtFirst) {
        if (params.size() > 1)
            throw new Error("Don't support search params " + params);
        if (resourceType != Patient.class)
            throw new Error("Don't support search on resources other than Patient (search was for " + resourceType.getSimpleName() + ")" );
        String param = params.get(0);
        String[] parts = param.split("=");
        if (parts.length != 2)
            throw new Error("Don't understand params = " + param);
        if (!parts[0].equals("identifier"))
            throw new Error("Don't support param " + parts[0]);
        String systemAndId = parts[1];
        if (!systemAndId.contains("|"))
            throw new Error("Param format (" + systemAndId + ") not supported");
        String[] sparts = systemAndId.split("\\|");
        String system = sparts[0];
        String id = sparts[1];

        List<ResourceWrapper> results = new ArrayList<>();
        for (Ref ref : caches.keySet()) {
            ResourceCache cache = caches.get(ref);
            List<ResourceWrapper> all = cache.getAll(base, resourceType.getSimpleName());
            if (stopAtFirst && !all.isEmpty()) {
                results.add(all.get(0));
                return results;
            }
            results.addAll(all);
        }
        return results;
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
