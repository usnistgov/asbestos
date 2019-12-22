package gov.nist.abestos.client.Base;

import gov.nist.asbestos.client.Base.DocumentCache;
import gov.nist.asbestos.client.Base.EC;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentCacheTest {
    private static EC ec;

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(DocumentCacheTest.class.getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        File externalCache = findExternalCache();
        ec = new EC(externalCache);
    }

    @Test
    void first() {
        DocumentCache cache = new DocumentCache(ec);
        String doc1 = "Hello World!";
        String doc2 = "Good morning";
        cache.clean();

        String id1 = cache.putDocumentCache(doc1.getBytes(), "text/plain");
        assertEquals("1", id1);

        String id2 = cache.putDocumentCache(doc2.getBytes(), "text/plain");
        assertEquals("2", id2);

        String doc1a = new String(cache.getDocumentFromCache(id1));
        assertEquals(doc1, doc1a);

        String type = cache.getDocumentTypeFromCache(id1);
        assertEquals("text/plain", type);
    }
}
