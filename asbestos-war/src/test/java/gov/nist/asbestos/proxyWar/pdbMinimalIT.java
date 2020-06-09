package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.events.UITask;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class pdbMinimalIT {
    private static final String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    static {
        try {
            base = new URI("http://localhost:" + proxyPort + "/asbestos/proxy/default__limited");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // No_Patient must be loaded (it's in the Test_Patients collection) for translation on limited
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        loadCaches();
    }

    static void loadCaches() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/selftest/default__default/Test_Patients/run";

        HttpGet getter = new HttpGet();
        getter.get(url);
        assertEquals(200, getter.getStatus());
    }

    // This test is run through the API because cache management is handled on the server so
    // the test engine must run there.
    @Test
    void sendPDB() throws URISyntaxException {
        String channelId = "limited";
        String collectionId = "Internal";
        String testId = "sendMinimalPDB";
        Map<String, Object> testReport = runTest(channelId, collectionId, testId);

        String eventId = getEventId(testReport);
        Report report = getAnalysis(channelId, eventId, "request");
        assertEquals("DocumentManifest", report.getBase().getName());
        assertTrue(report.getBase().isMinimal());
        assertFalse(report.getBase().isComprehensive());
        assertTrue(report.getBase().getMinimalChecked().size() > 3);
        assertFalse(report.getBase().getValidationResult().hasIssue());
        assertEquals(1, report.getObjects().size());

        RelatedReport drReport = report.getObjects().get(0);
        assertEquals("DocumentReference", drReport.getName());
        assertTrue(drReport.isMinimal());
        assertTrue(drReport.getMinimalChecked().size() > 4);
        assertFalse(drReport.isComprehensive());
        assertTrue(drReport.getComprehensiveErrors().size() > 5);
        assertFalse(drReport.getValidationResult().hasIssue());

        UIEvent event = getEvent(channelId, eventId);
        UITask task = event.getClientTask();
        String returnBundleString = task.getResponseBody();
        Bundle bundle = (Bundle) ProxyBase.parse(returnBundleString, Format.JSON);
        Bundle.BundleEntryComponent entry = bundle.getEntry().get(2);
        Bundle.BundleEntryResponseComponent response = entry.getResponse();
        String binaryUrl = response.getLocation();

        HttpGet getter = new HttpGet();
        getter.getJson(binaryUrl);
        assertEquals(200, getter.getStatus());


        String foo = "foo";
    }

    private String getEventId(Map<String, Object> report) {
        List<?> tests = (List<?>) report.get("test");
        Map<String, Object> test = (Map<String, Object>) tests.get(0);
        List<?> actions = (List<?>) test.get("action");
        Map<String, Object> operations = (Map<String, Object>) actions.get(0);
        Map<String, Object> operation = (Map<String, Object>) operations.get("operation");
        String detail = (String) operation.get("detail");
        String[] parts = detail.split("/");
        return parts[parts.length - 1];
    }

    private UIEvent getEvent(String channelId, String eventId) throws URISyntaxException {
        String url = "http://localhost:"
                + proxyPort
                + "/asbestos/log/default"
                + "/" + channelId
                + "/null"
                + "/" + eventId;

        HttpGet getter = new HttpGet();
        getter.getJson(url);
        assertEquals(200, getter.getStatus());

        return new Gson().fromJson(getter.getResponseText(), UIEvent.class);
    }

    private Report getAnalysis(String channelId, String eventId, String request_or_response) throws URISyntaxException {
        String url = "http://localhost:"
                + proxyPort
                + "/asbestos/log/analysis/event/default"
                + "/" + channelId
                + "/" + eventId
                + "/" + request_or_response;

        HttpGet getter = new HttpGet();
        getter.get(url);
        assertEquals(200, getter.getStatus());
        return new Gson().fromJson(getter.getResponseText(), Report.class);
    }


    private Map<String, Object> runTest(String channelId, String collectionId, String testId) throws URISyntaxException {
        String url = "http://localhost:"
                + proxyPort
                + "/asbestos/engine/testrun/default__"
                + channelId
                + "/" + collectionId
                + "/" + testId;

        HttpPost poster = new HttpPost();
        poster.setUri(new URI(url));
        poster.post();
        assertEquals(200, poster.getStatus());
        Map<String, Object> tests = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertTrue(tests.size() == 1);
        Map<String, Object> atts = (Map<String, Object>) tests.values().iterator().next();
        assertTrue(atts.size() > 4);
        assertEquals("pass", atts.get("result"));
        return atts;
    }

}
