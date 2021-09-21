package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test works with ec_shared EC_DIR (Tomcat launch) but not asbts-it-tests EC_DIR (using Maven command-line).
 */
class pdbCompIT {
    private static String testSession = "default";
    private static String channelId = "xds";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();
    private static URI base;

    static {
        try {
            base = new URI("http://localhost:" + proxyPort + "/asbestos/proxy/default__limited");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // Static Patients must be loaded (it's in the Test_Patients collection)
    @BeforeAll
    static void loadCaches() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/selftest/default__default/Test_Patients/run";

        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
    }


    // This test is run through the API because cache management is handled on the server so
    // the test engine must run there.
    @Test
    void sendPDB() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/testrun/default__xds/Internal/sendCompPDB";

        HttpPost poster = new HttpPost();
        poster.setUri(new URI(url));
        poster.post();
        assertEquals(200, poster.getStatus());
        Map<String, Object> tests = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertTrue(tests.size() == 1);
        Map<String, String> atts = (Map<String, String>) tests.values().iterator().next();
        assertTrue(atts.size() > 4);
        assertEquals("pass", atts.get("result"));
    }
}
