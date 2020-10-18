package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpGetter;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysePDBIT {

    private static final String proxyPort = ITConfig.getProxyPort();

    private static URI base;
    private static File external_cache;

    static String goodPdbLogUrl;
    static String noProfileLogUrl;
    static Map<String, Object> goodTestReport;
    static String drUrl;
    static Map<String, Object> noProfileTestReport;
    static String channelName = "limited";
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
                + channelName + "/" + eventId + "/request?validation=true";
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
                + channelName + "/" + eventId + "/request?focusUrl=urn:uuid:1e404af3-077f-4bee-b7a6-a9be97e1ce01";
        result = runAnalysis(analyseUrl);

        printErrors(result);
        assertEquals(0, getErrors(result).size());
    }

    @Test
    void analysePDBDRResponse() throws URISyntaxException {
        String url = goodPdbLogUrl;
        Map<String, Object> testReport = goodTestReport;
        String logUrl = (String) testReport.get("logUrl");
        ResourceWrapper logWrapper = new ResourceWrapper();
        UIEvent uiEventBase = new UIEvent(new EC(external_cache));
        uiEventBase.setHostPort("localhost:" + proxyPort);  // ServiceProperties access from outside war broken.
        UIEvent uiEvent = uiEventBase.fromURI(new URI(logUrl));
        logWrapper.setEvent(uiEvent, false);
        BaseResource resource = uiEvent.getResponseResource();
        assertTrue(resource instanceof Bundle);
        Bundle bundle = (Bundle) resource;
        List<String> drLocations = locations(bundle, "DocumentReference");
        assertEquals(1, drLocations.size());
        String[] parts = url.split("/");
        String eventId = parts[8];
        String analyseUrl;
        Map<String, Object> result;
        // focus on DR
        analyseUrl = "http://localhost:" + proxyPort + "/asbestos/log/analysis/url?url=" + drLocations.get(0);
        result = runAnalysis(analyseUrl);

        printErrors(result);
        assertEquals(0, getErrors(result).size());
    }

    List<String> locations(Bundle bundle, String type) {
        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, bundle.getType());
        List<String> locations = new ArrayList<>();

        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (comp.hasResponse() && comp.getResponse().hasLocation()) {
                Ref ref = new Ref(comp.getResponse().getLocation());
                if (type.equals(ref.getResourceType()))
                    locations.add(comp.getResponse().getLocation());
            }
        }

        return locations;
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
                + channelName + "/" + eventId + "/response?validation=false";
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
                + channelName + "/" + eventId + "/request?validation=false";
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

        loadCaches();

        loadEC();

        String testId;
        testId = "sendMinimalPDB";
        goodTestReport = Utility.runTest(channelName, collectionId, testId, null);
        goodPdbLogUrl = (String) goodTestReport.get("logUrl");
        UIEvent baseEvent = new UIEvent(new EC(external_cache));
        UIEvent event = baseEvent.fromURI(new URI(goodPdbLogUrl));
        BaseResource resource = event.getResponseResource();
        assertNotNull(resource);
        assertTrue(resource instanceof Bundle);
        Bundle bundle = (Bundle) resource;
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (comp.hasResponse() && comp.getResponse().hasLocation() && comp.getResponse().getLocation().contains("DocumentReference")) {
                drUrl = comp.getResponse().getLocation();
            }
        }


        testId = "Missing_Profile";
        noProfileTestReport = Utility.runTest(channelName, collectionId, testId, "PDBFails");
        noProfileLogUrl = (String) noProfileTestReport.get("logUrl");
    }

    static void loadCaches() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/selftest/default__default/Test_Patients/run";

        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
    }

    static void loadEC() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/log/ec";
        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
        external_cache = new File(getter.getResponseText());
    }

}
