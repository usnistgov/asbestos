package gov.nist.asbestos.client.resolver;

import ca.uhn.fhir.context.FhirContext;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Local cache of FHIR resources
 */
public class FileSystemResourceCache implements ResourceCache {
    private static final Logger logger = Logger.getLogger(FileSystemResourceCache.class);
    private static FhirContext ctx = FhirContext.forR4();

    private List<File> cacheDirs = new ArrayList<>();
    private Ref base = new Ref("");

    public FileSystemResourceCache(File cacheDir) {
        this.cacheDirs.add(cacheDir);
        File propFile = new File(cacheDir, "cache.properties");
        if (!propFile.exists())
            throw new RuntimeException(cacheDir + "/cache.properties does not exist");
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(propFile);
            props.load(is);
        } catch (Exception e) {
            throw new Error(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    ;
                }
            }
        }
        String aBase = props.getProperty("baseUrl");
        base = new Ref(aBase);
        logger.info("New Resource cache: " + base + " --> " + cacheDir);
    }

//    public FileSystemResourceCache(File cacheDir, Ref base) {
//        this.cacheDirs.add(cacheDir);
//        this.base = base;
//    }

    public FileSystemResourceCache() {

    }

    public List<File> getCacheDirs() {
        return cacheDirs;
    }

    public void insertCache(File cacheDir) { this.cacheDirs.add(0, cacheDir); }

    public void addCache(File cacheDir) {
        this.cacheDirs.add(cacheDir);
    }

    public ResourceWrapper readResource(Ref url) {
        File file = cacheFile(url, "xml");
        if (file != null) {
            String id = file.getName();
            id = id.substring(0, id.indexOf(".xml"));
            if (file.exists()) {
                ResourceWrapper wrapper = new ResourceWrapper(ctx.newXmlParser().parseResource(fileToString(file)));
                wrapper.setRef(url);
                wrapper.getResource().setId(id);
                return wrapper;
            }
        }
        file = cacheFile(url, "json");
        if (file == null)
            return null;
            //throw new Error("Cache resource does not exist:" + url);
        String id = file.getName();
        id = id.substring(0, id.indexOf(".json"));
        if (file.exists()) {
            ResourceWrapper wrapper = new ResourceWrapper(ctx.newJsonParser().parseResource(fileToString(file)));
            wrapper.setRef(url);
            wrapper.getResource().setId(id);
            return wrapper;
        }
        return null;
    }

    private ResourceWrapper readFile(File file) {
        if (file.getName().endsWith("xml")) {
            return new ResourceWrapper(ctx.newXmlParser().parseResource(fileToString(file)));
        } else if (file.getName().endsWith("json")) {
            return new ResourceWrapper(ctx.newJsonParser().parseResource(fileToString(file)));
        }
        else
            return null;
    }

    public List<ResourceWrapper> getAll(Ref base, String type) {
        List<ResourceWrapper> all = new ArrayList<>();

        for (File cacheDir : cacheDirs) {
            File dir = new File(cacheDir, type);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (!(name.endsWith(".xml") || name.endsWith(".json")))
                        continue;
                    ResourceWrapper wrapper = readFile(file);
                    String[] parts = file.getName().split("\\.", 2);
                    String id = parts[0];
                    wrapper.setRef(new Ref(base, type, id, null));
                    all.add(wrapper);
                }
            }
        }
        return all;
    }

    private String fileToString(File file) {
        InputStream  is = null;
        try {
            is = new FileInputStream(file);
            return IOUtils.toString(is, "UTF-8");
        } catch (Exception e) {
            if (is != null)
                try {
                    is.close();
                } catch (Exception e1) {

                }
            throw new Error(e);
        }
    }

    // TODO implement
    @Override
    public void add(Ref ref, ResourceWrapper resource) {

    }

    private File cacheFile(Ref relativeUrl, String fileType) {
        Ref base = relativeUrl.getBase();
        boolean isRelative = relativeUrl.isRelative();
        boolean hasBase = !base.toString().equals(relativeUrl.toString());
        String type = relativeUrl.getResourceType();
        String id = relativeUrl.getId() + ((fileType != null) ? "." + fileType : "");
        for (File cacheDir : cacheDirs) {
            File file;
            if (hasBase && isRelative)
                file = new File(new File(new File(cacheDir, base.toString()), type), id);
            else
                file = new File(new File(cacheDir, type), id);
            if (file.exists())
                return file;
        }
        return null;
    }

    public Ref getBase() {
        return base;
    }
}
