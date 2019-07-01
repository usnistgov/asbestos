package gov.nist.asbestos.mhd.resolver;

import gov.nist.asbestos.client.resolver.FileSystemResourceCache;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemResourceCacheTest {

    @Test
    void cacheTest() throws URISyntaxException {
        File cacheFile = Paths.get(getClass().getResource("/gov/nist/asbestos/mhd/resolver/cache/cache.properties").toURI()).getParent().toFile();

        FileSystemResourceCache cache = new FileSystemResourceCache(cacheFile);
        Ref ref = new Ref("http://localhost:8080/fhir/Patient/a2");
        ResourceWrapper resource = cache.readResource(ref);

        assertNotNull(resource);
        assertNotNull(resource.getRef());
        assertNotNull(resource.getResource());
        assertTrue(resource.isLoaded());
        assertTrue(resource.getResource() instanceof Patient);
        assertEquals("a2", resource.getResource().getIdElement().getValue());

    }
}
