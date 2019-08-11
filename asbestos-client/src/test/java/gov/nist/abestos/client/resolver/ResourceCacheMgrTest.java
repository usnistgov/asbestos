package gov.nist.abestos.client.resolver;

import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceCacheMgrTest {
    private static File externalCache;

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(ResourceCacheMgrTest.class.getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        externalCache = findExternalCache();
    }

}
