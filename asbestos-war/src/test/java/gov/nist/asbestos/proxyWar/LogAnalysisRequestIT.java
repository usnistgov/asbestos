package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration Tests for the LogAnalysis back end supporting the Inspector front end.
 * These tests mimic getLogEventAnalysis and getLogEventAnalysisForObject found in
 * https://github.com/usnistgov/asbestos/blob/master/asbestos-view/src/store/log.js
 */

public class LogAnalysisRequestIT {
    private static String testSession = "default";
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();
    private static EC ec;

    static URI base;
    static TestReport theReport;
    static String eventUrl;  // needs /request or /response appended before it points to a message
    static String logBase = "http://localhost:" + proxyPort + "/asbestos/log";
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
    static void getEc() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(logBase + "/ec");
        assertEquals(200, getter.getStatus());
        String ecDirName = getter.getResponseText();
        File ecDir = new File(ecDirName);
        assertTrue(ecDir.exists());
        assertTrue(ecDir.isDirectory());
        ec = new EC(ecDir);
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
        assertEquals(getEventId().split("_").length, 7);

        analysisBuilder.setTestSession(getTestSession());
        analysisBuilder.setChannelName(getChannelId());
        analysisBuilder.setEventId(getEventId());
    }

    // Get analysis of DocumentReference Resource based on its server URL.
    @Test
    void serverResource() throws URISyntaxException {
        String eventId = analysisBuilder.eventId;
        assertNotNull(eventId);
        UIEvent uiEvent = new UIEvent(ec).fromParms(
                testSession,
                channelId,
                "Bundle",
                eventId);
        assertNotNull(uiEvent);
        List<Ref> locations = getLocationsFromEvent(uiEvent);
        assertEquals(locations.size(), 3);

        Ref docRefRef = null;
        for (Ref location : locations) {
            if (location.toString().contains("DocumentReference"))
                docRefRef = location;
        }
        assertNotNull(docRefRef);

        String analysisUrl = analysisBuilder.getObjectAnalysis(docRefRef);
        HttpGetter getter = new HttpGetter();
        getter.get(analysisUrl);

        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        assertEquals("DocumentReference",
                ((Map)response.get("base"))
                        .get("name")
        );
        List objectList = (List) response.get("objects");
        assertEquals(2,
                objectList.size()
        );
        Map<String, Object> object1 = (Map<String, Object>) objectList.get(0);
        Map<String, Object> object2 = (Map<String, Object>) objectList.get(1);
        Map<String, Object> binary = "Binary".equals(object1.get("name")) ? object1 : object2;
        Map<String, Object> patient = "Patient".equals(object1.get("name")) ? object1 : object2;
        assertEquals("Binary", binary.get("name"));
        // this patient is sourcePatient which is contained within DocumentReference
        assertEquals("Patient", patient.get("name"));
    }

    List<Ref> getLocationsFromEvent(UIEvent event) {
        List<Ref> locations = new ArrayList<>();
        String responseBodyString = event.getResponseBody();
        Headers responseHeaders = event.getResponseHeader();

        BaseResource baseResource;
        baseResource = ParserBase.parse(responseBodyString, Format.fromContentType(responseHeaders.getContentType().getValue()));
        assertTrue(baseResource instanceof Bundle);
        Bundle bundle = (Bundle) baseResource;

        Bundle.BundleLinkComponent bundleLinkComponent = bundle.getLink("self");
        assertTrue(bundleLinkComponent.hasUrl());
        String baseUrl = bundleLinkComponent.getUrl();
        assertNotNull(baseUrl);
        for( Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {
            String componentUrl = bundleEntryComponent.getResponse().getLocation();
            assertNotNull(componentUrl);
            Ref url = new Ref(componentUrl).rebase(baseUrl);
            locations.add(url);
        }

        return locations;
    }

    // Analyse the request (static) message without offering a focusUrl so the focus defaults to the DocumentManifest
    // The analysis should find a DocumentManifest and a DocumentReference.
    @Test
    void eventNoFocusRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(analysisBuilder.getEventRequestAnalysis(null, true, false, false));
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

    // Analyse the request with a focus on the DocumentManifest.  Should find the DocumentManifect
    // and DocumentReference.
    @Test
    void eventWithDocumentManifestFocusUrlRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();

        getter.get(analysisBuilder.getEventRequestAnalysis(documentManifestRequestUrl, true, false, false));
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

    // Analyse the request with a focus on the DocumentReference.  Should find the DocumentReference,
    // Binary, and Patient.
    @Test
    void eventWithDocumentReferenceFocusUrlRequest() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();
        getter.get(analysisBuilder.getEventRequestAnalysis(documentReferenceRequestUrl, true, false, false));
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

    // Analyse a response with a default focus (DocumentManifest).
    @Test
    void eventNoFocusResponse() throws URISyntaxException {
        HttpGetter getter = new HttpGetter();

        getter.get(analysisBuilder.getEventResponseAnalysis());
        assertEquals(200, getter.getStatus());
        Map<String, Object>  response = new Gson().fromJson(getter.getResponseText(), Map.class);
        Map<String, Object> base = (Map)response.get("base");

        assertEquals("DocumentManifest", base.get("name"));
        String dmUrl = (String) base.get("url");
        assertTrue(dmUrl.startsWith("http"));
        Ref dm = new Ref(dmUrl);
        assertTrue(dm.toString().contains("asbestos/proxy"));
        assertEquals(0, dm.getParameterNames().size());
        List objectList = (List) response.get("objects");
        assertEquals(1, objectList.size());


        Map<String, Object> object1 = (Map<String, Object>) objectList.get(0);
        String name1 = (String) object1.get("name");

        Map<String, Object> docRef = "DocumentReference".equals(name1) ? object1 : null;
        assertNotNull(docRef);
    }

    static class AnalysisBuilder {
        String testSession = null;
        String channelName = null;
        String testCollectionId = null;
        String testId = null;
        String url = null;
        String focusUrl = null;
        String eventId = null;
        String fixturePath = null;
        boolean useProxy = false;
        boolean ignoreBadRefs = false;

        public String getEventRequestAnalysis(String focusUrl, boolean useProxy, boolean useGzip, boolean ignoreBadRefs) {
            String url =
                    logBase
                            + "/" + "analysis"
                            + "/" + "event"
                            + "/" + testSession
                            + "/" + channelName
                            + "/" + eventId
                            + "/" + "request";
            if (focusUrl != null) {
                url = url + "?focusUrl=" + focusUrl;
            }
            return url;
        }

        // focusUrl is usable on response.  Just hard to setup test data for IT test.
        public String getEventResponseAnalysis() {
            return
                    logBase
                            + "/" + "analysis"
                            + "/" + "event"
                            + "/" + testSession
                            + "/" + channelName
                            + "/" + eventId
                            + "/" + "response";
        }

        public String getObjectAnalysis(Ref ref) {
            return
                    logBase
                    + "/" + "analysis"
                    + "/" + "url"
                    + "?url=" + ref.toString();
        }

        public String getTestSession() {
            return testSession;
        }

        public AnalysisBuilder setTestSession(String testSession) {
            this.testSession = testSession;
            return this;
        }

        public String getChannelName() {
            return channelName;
        }

        public AnalysisBuilder setChannelName(String channelName) {
            this.channelName = channelName;
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
