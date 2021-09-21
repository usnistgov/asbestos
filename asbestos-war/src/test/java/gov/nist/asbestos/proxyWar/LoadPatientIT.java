package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.http.operations.HttpPost;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Use unique patient so as to not to conflict with the Self-Test Patient loading tests.
 */
public class LoadPatientIT {
    private static final String testSession = "default";
    private static final String channelId = "IT";
    private static final String fhirPort = ITConfig.getFhirPort();
    private static final String proxyPort = ITConfig.getProxyPort();

    @Test
    void nonZip() throws URISyntaxException, IOException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/"
                + "testrun/default__default/IT_Test_Patients/Mickey_Mouse?_gzip=false";

        HttpPost poster = new HttpPost();
        poster.postJson(new URI(url), null);
        assertEquals(200, poster.getStatus());

        Map<String, Map<String, String>> myMap = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertEquals(1, myMap.size());

        Map<String, String> testReport = myMap.get("Mickey_Mouse");
        assertTrue(testReport.size() > 3);
        assertTrue(testReport.containsKey("resourceType"));
    }


    @Test
    void zip() throws URISyntaxException, IOException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/"
                + "testrun/default__default/IT_Test_Patients/Mickey_Mouse?_gzip=true";

        HttpPost poster = new HttpPost();
        poster.postJson(new URI(url), null);
        assertEquals(200, poster.getStatus());

        Map<String, Map<String, String>> myMap = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertEquals(1, myMap.size());

        Map<String, String> testReport = myMap.get("Mickey_Mouse");
        assertTrue(testReport.size() > 3);
        assertTrue(testReport.containsKey("resourceType"));
    }

}
