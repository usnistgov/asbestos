package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver

import ca.uhn.fhir.context.FhirContext
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import groovy.transform.TypeChecked
import org.apache.log4j.Logger
/**
 * Local cache of FHIR resources
 */
@TypeChecked
class FileSystemResourceCache implements ResourceCache {
    private static final Logger logger = Logger.getLogger(FileSystemResourceCache.class)
    static FhirContext ctx = FhirContext.forR4()

    private File cacheDir
    Ref base

    FileSystemResourceCache(File cacheDir) {
        this.cacheDir = cacheDir
        File propFile = new File(cacheDir, 'cache.properties')
        assert propFile.exists() : "${cacheDir}/cache.properties does not exist"
        Properties props = new Properties()
        propFile.withInputStream { InputStream is -> props.load(is) }
        String aBase = props.getProperty('baseUrl')
        base = new Ref(aBase)
        logger.info("New Resource cache: ${base}  --> ${cacheDir}")
    }

    ResourceWrapper readResource(Ref url) {
        File file = cacheFile(url, 'xml')
        if (file.exists())
            return new ResourceWrapper(ctx.newXmlParser().parseResource(file.text))
        file = cacheFile(url, 'json')
        if (file.exists())
            return new ResourceWrapper(ctx.newJsonParser().parseResource(file.text))
        return null
    }

    // TODO implement
    @Override
    void add(Ref ref, ResourceWrapper resource) {

    }

    private File cacheFile(Ref relativeUrl, fileType) {
        assert !relativeUrl.isAbsolute()
        String type = relativeUrl.resourceType
        String id = relativeUrl.id + ((fileType) ? ".${fileType}" : '')
        return new File(new File(cacheDir, type), id)
    }
}
