package gov.nist.asbestos.mhd.resolver;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceCacheMgrTest {
    @Test
    void cacheTest() throws URISyntaxException {
        File externalCache = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent().toFile();

        ResourceCacheMgr mgr = new ResourceCacheMgr(externalCache);
        Ref ref = new Ref("http://localhost:8080/fhir/Patient/a2");
        ResourceWrapper resource = mgr.getResource(ref);

        assertNotNull(resource);
        assertNotNull(resource.getUrl());
        assertNotNull(resource.getResource());
        assertTrue(resource.isLoaded());
        assertTrue(resource.getResource() instanceof Patient);

    }
}
