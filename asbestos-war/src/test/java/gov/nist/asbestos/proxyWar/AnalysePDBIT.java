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
        String analyseUrl;

        analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/event/default/"
                + channelId + "/" + eventId + "/request?validation=true";
        Map<String, Object> result;

        result = runAnalysis(analyseUrl);

        assertEquals("DocumentManifest", baseName(result));

        assertEquals(1, getObjectCount(result));
        assertEquals("DocumentReference", getAtt(getObject(result, 0), "name"));
    }

    @Test
    void analysePDBDRRequest() throws URISyntaxException {
        String url = goodPdbLogUrl;
        String[] parts = url.split("/");
        String eventId = parts[8];
        String analyseUrl;
        Map<String, Object> result;
        // focus on DR
        analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/event/default/"
                + channelId + "/" + eventId + "/request?focusUrl=urn:uuid:1e404af3-077f-4bee-b7a6-a9be97e1ce01";
        result = runAnalysis(analyseUrl);

        printErrors(result);
        assertEquals(0, getErrors(result).size());
    }

    Map<String, Object> runAnalysis(String analyseUrl) throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(analyseUrl);
        assertEquals(200, getter.getStatus());
        Map<String, Object> result = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertNotNull(result);
        return result;
    }

    String baseName(Map<String, Object> result) {
        Map<String, Object> base = (Map<String, Object>) result.get("base");
        return (String) base.get("name");
    }

    List<Map<String, Object>> getObjects(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("objects");
    }

    List<String> getErrors(Map<String, Object> result) {
        return (List<String>) result.get("errors");
    }

    void printErrors(Map<String, Object> result) {
        for (String error : getErrors(result)) {
            System.out.println(error);
        }
    }

    Map<String, Object> getObject(Map<String, Object> result, int i) {
        return getObjects(result).get(i);
    }

    String getAtt(Map<String, Object> result, String name) {
        return (String) result.get(name);
    }

    int getObjectCount(Map<String, Object> result) {
        return getObjects(result).size();
    }

    Map<String, Object> getObjectByName(Map<String, Object> result, String theName) {
        List<Map<String, Object>> objects = getObjects(result);
        for (Map<String, Object> obj : objects) {
            String name = (String) obj.get("name");
            if (theName.equals(name))
                return obj;
        }
        return null;
    }

    @Test
    void analysePDBResponse() throws URISyntaxException {
        String url = goodPdbLogUrl;
        String[] parts = url.split("/");
        String eventId = parts[8];
        String analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/event/default/"
                + channelId + "/" + eventId + "/response?validation=false";
        HttpGetter getter = new HttpGetter();
        getter.get(analyseUrl);
        assertEquals(200, getter.getStatus());
        Map<String, Object> result = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertNotNull(result);
        assertEquals(3, ((List)result.get("objects")).size());
        assertTrue(includesType(result, "DocumentReference"));
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
        assertTrue(includesType(result, "DocumentReference"));
        assertTrue(includesType(result, "DocumentManifest"));
        assertTrue(includesType(result, "Binary"));
    }

    private boolean includesType(Map<String, Object> result, String typeName) {
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
