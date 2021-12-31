package gov.nist.asbestos.client.resolver;

import java.util.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class CacheBundle {
    private static final Logger logger = Logger.getLogger(CacheBundle.class.getName());
    MemoryResourceCache mem = null;
    FileSystemResourceCache file = null;

    CacheBundle() {

    }

    CacheBundle(MemoryResourceCache mcache) {
        mem = mcache;
    }

    CacheBundle(File dir, Ref baseUrl) {
//            file = new FileSystemResourceCache(dir, baseUrl);
        file = new FileSystemResourceCache();
        file.addCache(dir);
        mem = new MemoryResourceCache();
    }

    CacheBundle(File cacheDir) {
        if (cacheDir.exists() && cacheDir.isDirectory()  && new File(cacheDir, "cache.properties").exists()) {
            logger.info("Scanning Resource Cache directory " + cacheDir);
            CacheBundle bundle = new CacheBundle();
            file = new FileSystemResourceCache(cacheDir);
            mem = new MemoryResourceCache();
        }
    }

    void insertFileCache(File cacheDir) {
        file.insertCache(cacheDir);
    }

    List<File> getFileSystemCacheDirs() {
        if (file == null)
            return new ArrayList<>();
        return file.getCacheDirs();
    }

    Ref getBase() {
        if (file == null)
            return null;
        return file.getBase();
    }

    ResourceWrapper getResource(Ref fullUrl) {
        if (mem.hasResource(fullUrl))
            return mem.readResource(fullUrl);
        return file.readResource(fullUrl);
    }

    List<ResourceWrapper> getAll(Ref base, String resourceType) {
        List<ResourceWrapper> result = new ArrayList<>();
        result.addAll(mem.getAll(base, resourceType));
        result.addAll(file.getAll(base, resourceType));
        return result;
    }
}
