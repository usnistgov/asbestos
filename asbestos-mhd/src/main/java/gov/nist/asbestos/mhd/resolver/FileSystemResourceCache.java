package gov.nist.asbestos.mhd.resolver;

import ca.uhn.fhir.context.FhirContext;

import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Local cache of FHIR resources
 */
public class FileSystemResourceCache implements ResourceCache {
    private static final Logger logger = Logger.getLogger(FileSystemResourceCache.class);
    private static FhirContext ctx = FhirContext.forR4();

    private File cacheDir;
    private Ref base;

    public FileSystemResourceCache(File cacheDir) {
        this.cacheDir = cacheDir;
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

    public ResourceWrapper readResource(Ref url) {
        File file = cacheFile(url, "xml");
        String id = file.getName();
        id = id.substring(0, id.indexOf(".xml"));
        if (file.exists()) {
            ResourceWrapper wrapper = new ResourceWrapper(ctx.newXmlParser().parseResource(fileToString(file)));
            wrapper.setUrl(url);
            wrapper.getResource().setId(id);
            return wrapper;
        }
        file = cacheFile(url, "json");
        id = file.getName();
        id = id.substring(0, id.indexOf(".json"));
        if (file.exists()) {
            ResourceWrapper wrapper = new ResourceWrapper(ctx.newJsonParser().parseResource(fileToString(file)));
            wrapper.setUrl(url);
            wrapper.getResource().setId(id);
            return wrapper;
        }
        return null;
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
        String type = relativeUrl.getResourceType();
        String id = relativeUrl.getId() + ((fileType != null) ? "." + fileType : "");
        return new File(new File(cacheDir, type), id);
    }

    public Ref getBase() {
        return base;
    }
}
