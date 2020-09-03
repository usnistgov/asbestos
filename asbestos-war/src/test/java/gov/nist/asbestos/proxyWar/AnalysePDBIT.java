package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.http.operations.HttpGetter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysePDBIT {

    private static final String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    static String goodPdbLogUrl;
    static String noProfileLogUrl;
    static String channelId = "limited";
    static String collectionId = "Internal";

    static {
        try {
            base = new URI("http://localhost:" + proxyPort + "/asbestos/proxy/default__limited");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    void analysePDBRequest() throws URISyntaxException {
        String url = goodPdbLogUrl;
        String[] parts = url.split("/");
        String eventId = parts[8];
        String analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/event/default/"
                + channelId + "/" + eventId + "/request?validation=false";
        HttpGetter getter = new HttpGetter();
        getter.get(analyseUrl);
        assertEquals(200, getter.getStatus());
        Map<String, Object> result = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertNotNull(result);
        assertEquals(2, ((List)result.get("objects")).size());
        assertTrue(hasType(result, "DocumentReference"));
        assertTrue(hasType(result, "Binary"));
    }

    @Test
    void analyseNoProfileRequest() throws URISyntaxException {
        String url = noProfileLogUrl;
        String[] parts = url.split("/");
        String eventId = parts[8];
        String analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/event/default/"
                + channelId + "/" + eventId + "/request?validation=false";
        HttpGetter getter = new HttpGetter();
        getter.get(analyseUrl);
        assertEquals(200, getter.getStatus());
        Map<String, Object> result = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertNotNull(result);
        assertEquals(3, ((List)result.get("objects")).size());
        assertTrue(hasType(result, "DocumentReference"));
        assertTrue(hasType(result, "DocumentManifest"));
        assertTrue(hasType(result, "Binary"));
    }

    private boolean hasType(Map<String, Object> result, String typeName) {
        List<Object> objects = (List<Object>) result.get("objects");
        for (Object o : objects) {
            Map<String, Object> map = (Map<String, Object>) o;
            String name = (String) map.get("name");
            if (typeName.equals(name))
                return true;
        }
        return false;
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        Map<String, Object> testReport;

        loadCaches();

        String testId;
        testId = "sendMinimalPDB";
        testReport = Utility.runTest(channelId, collectionId, testId, null);
        goodPdbLogUrl = (String) testReport.get("logUrl");

        testId = "Missing_Profile";
        testReport = Utility.runTest(channelId, collectionId, testId, "PDBFails");
        noProfileLogUrl = (String) testReport.get("logUrl");
    }

    static void loadCaches() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/selftest/default__default/Test_Patients/run";

        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
    }

}
