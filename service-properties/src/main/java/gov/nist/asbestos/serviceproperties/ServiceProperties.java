package gov.nist.asbestos.serviceproperties;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * This class provides service registry properties.
 * NOTE: Only FhirToolkit (Asbestos) is intended to use this class.
 */
public class ServiceProperties {
    private static final Logger logger = Logger.getLogger(ServiceProperties.class);
    private static File spFile;
    private long spFileLastModified;
    private static Properties properties;
    private static ServiceProperties spClass;

    private ServiceProperties() throws Exception {
        String spString = System.getProperty("SERVICE_PROPERTIES");
        if (spString == null) {
            try {
                spFile = getLocalSpFile(getClass());
            } catch (Exception ex) {
                logger.error("Could not locate the service.properties file: " + ex.toString());
                throw ex;
            }
        } else {
            spFile = new File(spString);
        }
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() throws IOException {
        logger.info(String.format("*** Loading service.properties from: %s", spFile.toString()));
        properties.load(new FileInputStream(spFile));
        spFileLastModified = spFile.lastModified();
    }

    public static ServiceProperties getInstance() throws Exception {
        if (spClass == null) {
            spClass = new ServiceProperties();
        }
        return spClass;
    }

    private void reloadIfModified() {
        long lastModified = spFile.lastModified();
        if (lastModified > spFileLastModified) {
            try {
                loadProperties();
            } catch (IOException ioEx) {
                logger.warn("reloadIfModified failed: " + ioEx.toString());
            }
        }
    }

    public static File getLocalSpFile(Class clazz) throws URISyntaxException  {
        Objects.requireNonNull(clazz);
        return Paths.get(clazz.getResource("/").toURI()).resolve("service.properties").toFile();
    }

    /**
     * Returns a copy of the Properties object
     * @return Properties
     */
    public Properties getCopyOfProperties() {
        Properties copyOfProperties = new Properties();
        properties.forEach((k,v) -> copyOfProperties.setProperty((String)k,(String)v));
        return copyOfProperties;
    }

    public String getProperty(ServicePropertiesEnum key) {
        return getProperty(key.getKey());
    }

    public String getProperty(String key) {
        reloadIfModified();
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value)  {
        properties.setProperty(key, value);
    }

    public boolean removeProperty(String key) {
        try {
            properties.remove(key);
            return true;
        } catch (Exception ex) {
            logger.warn("Could not remove key: " + key);
            return false;
        }
    }

    public boolean save() throws IOException {
        if (spFile.exists() && spFile.canWrite()) {
            FileOutputStream fos = new FileOutputStream(spFile);
            try {
                properties.store(fos, "");
                fos.flush();
                spFileLastModified = spFile.lastModified();
                return true;
            } finally {
                fos.close();
            }
        }
        return false;
    }

}
