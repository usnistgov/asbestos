package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogAnalysisRequestIT {
    private static String testSession = "default";
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    static URI base;
    static TestReport theReport;
    static String eventUrl;  // needs /request or /response appended before it points to a message
    static String logBase = "http://localhost:8081/asbestos/log";
    static AnalysisBuilder analysisBuilder = new AnalysisBuilder();

    static String getTestSession() {
        String[] parts = eventUrl.split("/");
        return parts[5];
    }

    static String getChannelId() {
        String[] parts = eventUrl.split("/");
        return parts[6];
    }

    static String getResourceType() {
        String[] parts = eventUrl.split("/");
        return parts[7];
    }

    static String getEventId() {
        String[] parts = eventUrl.split("/");
        return parts[8];
    }

    @BeforeAll
    static void runATest() throws IOException, URISyntaxException {
        base = new URI(Utility.createChannel(testSession, channelId, fhirPort, proxyPort));

        TestEngine engine = Utility.run(base, "/logAnalysis/external_cache/FHIRTestCollections/collection1/test1/TestScript.xml");
        theReport = engine.getTestReport();
        assertEquals(TestReport.TestReportResult.PASS, theReport.getResult());
        eventUrl = theReport.getTest().get(0).getAction().get(0).getOperation().getDetail();

        assertTrue(eventUrl.startsWith("http"));
        assertTrue(eventUrl.contains("asbestos/log"));

        // self checks
        assertEquals(testSession, getTestSession());
        assertEquals(channelId, getChannelId());
        assertEquals("Bundle", getResourceType());
        assertTrue(getEventId().split("_").length == 7);

        analysisBuilder.setTestSession(getTestSession());
        analysisBuilder.setChannelId(getChannelId());
        analysisBuilder.setEventId(getEventId());
    }

    @Test
    void eventRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(analysisBuilder.getEventRequestAnalysis(null));
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertEquals("DocumentManifest",
                ((Map)response.get("base"))
                        .get("name")
        );
        List objectList = (List) response.get("objects");
        assertEquals(1,
                objectList.size()
        );
        Map<String, Object> objects = (Map<String, Object>) objectList.get(0);
        assertEquals("DocumentReference",
                objects.get("name")
        );
        // returned link to DocumentReference
        String drUrl = (String) objects.get("url");
        assertTrue(drUrl.startsWith("http"));
    }

    String documentManifestRequestUrl = "urn:uuid:3fdc72f4-a11d-4a9d-9260-a9f745779e02";
    String documentReferenceRequestUrl = "urn:uuid:1e404af3-077f-4bee-b7a6-a9be97e1ce01";

    @Test
    void eventWithDocumentManifestFocusUrlRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();

        getter.get(analysisBuilder.getEventRequestAnalysis(documentManifestRequestUrl));
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        Map<String, Object> base = (Map)response.get("base");

        assertEquals("DocumentManifest", base.get("name"));
        String dmUrl = (String) base.get("url");
        assertTrue(dmUrl.startsWith("http"));
        Ref dm = new Ref(dmUrl);
        assertEquals(documentManifestRequestUrl, dm.getFocusUrl());
        assertTrue(dm.toString().contains("asbestos/log"));
        assertEquals(1, dm.getParameterNames().size());
        List objectList = (List) response.get("objects");
        assertEquals(1,
                objectList.size()
        );

        Map<String, Object> objects = (Map<String, Object>) objectList.get(0);
        assertEquals("DocumentReference", objects.get("name"));
        // returned link to DocumentReference
        String drUrl = (String) objects.get("url");
        assertTrue(drUrl.startsWith("http"));
        Ref dr = new Ref(drUrl);
        assertEquals(documentReferenceRequestUrl, dr.getFocusUrl());
    }

    @Test
    void eventWithDocumentReferenceFocusUrlRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(analysisBuilder.getEventRequestAnalysis(documentReferenceRequestUrl));
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertEquals("DocumentReference",
                ((Map)response.get("base"))
                        .get("name")
        );
        Ref dr = new Ref((String) ((Map)response.get("base"))
                .get("url"));
        assertEquals(documentReferenceRequestUrl, dr.getFocusUrl());
        assertTrue(dr.toString().contains("asbestos/log"));

        List objectList = (List) response.get("objects");
        assertEquals(2, objectList.size());
        Map<String, Object> object1 = (Map<String, Object>) objectList.get(0);
        Map<String, Object> object2 = (Map<String, Object>) objectList.get(1);
        Map<String, Object> binary = "Binary".equals(object1.get("name")) ? object1 : object2;
        Map<String, Object> patient = "Patient".equals(object1.get("name")) ? object1 : object2;
        assertEquals("Binary", binary.get("name"));
        assertEquals("Patient", patient.get("name"));
    }

    @Test
    void eventResponse() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();

        getter.get(analysisBuilder.getEventResponseAnalysis());
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        Map<String, Object> base = (Map)response.get("base");

        assertEquals("Bundle", base.get("name"));
        String dmUrl = (String) base.get("url");
        assertTrue(dmUrl.startsWith("http"));
        Ref dm = new Ref(dmUrl);
        assertTrue(dm.toString().contains("asbestos/log"));
        assertEquals(0, dm.getParameterNames().size());
        List objectList = (List) response.get("objects");
        assertEquals(3, objectList.size());


        Map<String, Object> object1 = (Map<String, Object>) objectList.get(0);
        Map<String, Object> object2 = (Map<String, Object>) objectList.get(1);
        Map<String, Object> object3 = (Map<String, Object>) objectList.get(2);
        String name1 = (String) object1.get("name");
        String name2 = (String) object2.get("name");
        String name3 = (String) object3.get("name");

        Map<String, Object> binary = "Binary".equals(name1) ? object1 : "Binary".equals(name2) ? object2 : object3;
        Map<String, Object> docRef = "DocumentReference".equals(name1) ? object1 : "DocumentReference".equals(name2) ? object2 : object3;
        Map<String, Object> docMan = "DocumentManifest".equals(name1) ? object1 : "DocumentManifest".equals(name2) ? object2 : object3;
        assertEquals("Binary", binary.get("name"));
        assertEquals("DocumentReference", docRef.get("name"));
        assertEquals("DocumentManifest", docMan.get("name"));
    }

    @Test
    void urlRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        String analysisUrl = analysisBuilder.getUrlAnalysis(eventUrl) + "/request";
        getter.get(analysisUrl);
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertEquals("DocumentManifest",
                ((Map)response.get("base"))
                        .get("name")
        );
    }

    enum AnalysisType {
        STATIC, URL, EVENT;
    }

    static class AnalysisBuilder {
        String testSession = null;
        String channelId = null;
        String testCollectionId = null;
        String testId = null;
        String url = null;
        AnalysisType analysisType = null;
        String focusUrl = null;
        String eventId = null;
        String fixturePath = null;
        boolean useProxy = false;
        boolean ignoreBadRefs = false;

        public String getEventRequestAnalysis(String focusUrl) {
            String url =
                    logBase
                            + "/" + "analysis"
                            + "/" + "event"
                            + "/" + testSession
                            + "/" + channelId
                            + "/" + eventId
                            + "/" + "request";
            if (focusUrl != null) {
                url = url + "?focusUrl=" + focusUrl;
            }
            return url;
        }

        public String getEventResponseAnalysis() {
            String url =
                    logBase
                            + "/" + "analysis"
                            + "/" + "event"
                            + "/" + testSession
                            + "/" + channelId
                            + "/" + eventId
                            + "/" + "response";
            return url;
        }

        public String getUrlAnalysis(String url) {
            return
                    logBase
                    + "/" + "analysis"
                    + "/" + "url"
                    + "?url=" + url;
        }

        public String getTestSession() {
            return testSession;
        }

        public AnalysisBuilder setTestSession(String testSession) {
            this.testSession = testSession;
            return this;
        }

        public String getChannelId() {
            return channelId;
        }

        public AnalysisBuilder setChannelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public String getTestCollectionId() {
            return testCollectionId;
        }

        public AnalysisBuilder setTestCollectionId(String testCollectionId) {
            this.testCollectionId = testCollectionId;
            return this;
        }

        public String getTestId() {
            return testId;
        }

        public AnalysisBuilder setTestId(String testId) {
            this.testId = testId;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public AnalysisBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public AnalysisType getAnalysisType() {
            return analysisType;
        }

        public AnalysisBuilder setAnalysisType(AnalysisType analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public String getFocusUrl() {
            return focusUrl;
        }

        public AnalysisBuilder setFocusUrl(String focusUrl) {
            this.focusUrl = focusUrl;
            return this;
        }

        public String getEventId() {
            return eventId;
        }

        public AnalysisBuilder setEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public String getFixturePath() {
            return fixturePath;
        }

        public AnalysisBuilder setFixturePath(String fixturePath) {
            this.fixturePath = fixturePath;
            return this;
        }

        public boolean isUseProxy() {
            return useProxy;
        }

        public AnalysisBuilder setUseProxy(boolean useProxy) {
            this.useProxy = useProxy;
            return this;
        }

        public boolean isIgnoreBadRefs() {
            return ignoreBadRefs;
        }

        public AnalysisBuilder setIgnoreBadRefs(boolean ignoreBadRefs) {
            this.ignoreBadRefs = ignoreBadRefs;
            return this;
        }
    }
}
