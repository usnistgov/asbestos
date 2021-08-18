package gov.nist.abestos.client.resolver;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCacheMgrTest {
    private static File externalCache;
    private ResourceCacheMgr resourceCacheMgr;
    private FhirClient fhirClient;

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(ResourceCacheMgrTest.class.getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        externalCache = findExternalCache();
    }

    @BeforeEach
    void beforeEach() {
        resourceCacheMgr = new ResourceCacheMgr(externalCache);
        fhirClient = new FhirClient()
                .setResourceCacheMgr(resourceCacheMgr);
    }

    @Test
    void readFromStaticCacheTest() {
        Ref ref = new Ref("http://localhost:7080/fhir/Patient/a2");
        ResourceWrapper wrapper = fhirClient.readResource(ref);
        assertTrue(wrapper.isLoaded());

        assertTrue(resourceCacheMgr.getCachedServers().stream()
                .map(ref1 -> ref1.getBase().toString())
                .collect(Collectors.toList())
                .contains("http://localhost:7080/fhir"));
    }

    @Test
    void addToExistingMemoryCacheTest() {
        Patient patient1 = new Patient();
        Ref baseUrl = new Ref("http://localhost:7080/fhir");
        Ref ref = baseUrl.withResource("Patient").withNewId("j89");

        // add to cache
        resourceCacheMgr.add(ref, patient1);
        ResourceWrapper wrapper = fhirClient.readResource(ref);
        assertTrue(wrapper.isLoaded());

        // clear cache
        fhirClient.setResourceCacheMgr(new ResourceCacheMgr(externalCache));

        wrapper = fhirClient.readResource(ref);
        assertFalse(wrapper.isLoaded());
    }
}
