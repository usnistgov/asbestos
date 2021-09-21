package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.ModularEngine;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Utility {
    static final String proxyPort = ITConfig.getProxyPort();

    static URI base;

    static {
        try {
            base = new URI("http://localhost:" + proxyPort + "/asbestos/proxy/default__limited");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static void loadCaches() throws URISyntaxException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/selftest/default__default/Test_Patients/run";

        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
    }


    static String createChannel(String testSession, String channelName, String fhirPort, String proxyPort) throws URISyntaxException, IOException {
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession(testSession)
                .setChannelName(channelName)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("fhir")
                .setFhirBase("http://localhost:" + fhirPort + "/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/asbestos/rw/channel/create"), json);
        int status = poster.getStatus();
        assertTrue(status == 200 || status == 201, "POST to " + "http://localhost:" + proxyPort + "/asbestos/rw/channel/create");
        return "http://localhost:" + proxyPort + "/asbestos/proxy/" + testSession + "__" + channelName;
    }

    static TestEngine run(URI serverBase, String testScriptLocation) throws URISyntaxException {
        EC ec = new EC(ExternalCache.getExternalCache());
        Val val = new Val();
        File test1 = Paths.get(ConditionalIT.class.getResource(testScriptLocation).toURI()).getParent().toFile();

        File patientCacheDir = ec.getTestLogCacheDir("default__default");
        File alternatePatientCacheDir = ec.getTestLogCacheDir("default__default");
        patientCacheDir.mkdirs();
        alternatePatientCacheDir.mkdirs();


        ModularEngine modularEngine = new ModularEngine(test1, serverBase).setSaveLogs(true);
        TestEngine mainTestEngine = modularEngine.getMainTestEngine();
        modularEngine
                .setVal(val)
                .setTestSession(testScriptLocation)
                .setChannelId("default__default")
                .setExternalCache(ExternalCache.getExternalCache())
                .setFhirClient(new FhirClient())
                .addCache(patientCacheDir)
                .addCache(alternatePatientCacheDir)
                .runTest();
        int i = 0;
        for (TestEngine engine : modularEngine.getTestEngines()) {
            System.out.println("ENGINE " + i);
            System.out.println(engine.getTestReportAsJson());
            i++;
        }
        TestReport report = mainTestEngine.getTestReport();
        TestReport.TestReportResult result = report.getResult();
        //assertEquals(TestReport.TestReportResult.PASS, result);
        return mainTestEngine;
    }

    static String getEventId(Map<String, Object> report) {
        List<?> tests = (List<?>) report.get("test");
        Map<String, Object> test = (Map<String, Object>) tests.get(0);
        List<?> actions = (List<?>) test.get("action");
        Map<String, Object> operations = (Map<String, Object>) actions.get(0);
        Map<String, Object> operation = (Map<String, Object>) operations.get("operation");
        String detail = (String) operation.get("detail");
        String[] parts = detail.split("/");
        return parts[parts.length - 1];
    }

    static UIEvent getEvent(String channelId, String eventId) throws URISyntaxException {
        String url = "http://localhost:"
                + proxyPort
                + "/asbestos/log/default"
                + "/" + channelId
                + "/null"
                + "/" + eventId;

        HttpGetter getter = new HttpGetter();
        getter.getJson(url);
        assertEquals(200, getter.getStatus());

        return new Gson().fromJson(getter.getResponseText(), UIEvent.class);
    }

    static Report getAnalysis(String channelId, String eventId, String request_or_response) throws URISyntaxException {
        String url = "http://localhost:"
                + proxyPort
                + "/asbestos/log/analysis/event/default"
                + "/" + channelId
                + "/" + eventId
                + "/" + request_or_response;

        HttpGetter getter = new HttpGetter();
        getter.get(url);
        assertEquals(200, getter.getStatus());
        return new Gson().fromJson(getter.getResponseText(), Report.class);
    }


    static Map<String, Object> runTest(String channelId, String collectionId, String testId, String submissionSection) throws URISyntaxException {
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
        assertTrue(tests.size() > 0);
        Map<String, Object> atts = (Map<String, Object>) tests.values().iterator().next();
        assertTrue(atts.size() > 4);
        assertEquals("pass", atts.get("result"));

        String testName = submissionSection == null ? testId : testId + "/" + submissionSection;
        Object reportObj = tests.get(testName);
        String json = new Gson().toJson(reportObj);

        BaseResource resource = ParserBase.parse(json, Format.JSON);
        assertTrue(resource instanceof TestReport);
        TestReport report = (TestReport) resource;
        String url2 = report.getTest().get(0).getAction().get(0).getOperation().getDetail();
        atts.put("logUrl", url2);

        return atts;
    }

}
