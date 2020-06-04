package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpPost;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LoadPatientIT {
    private static String testSession = "default";
    private static String channelId = "IT";
    private static String fhirPort = ITConfig.getFhirPort();
    private static String proxyPort = ITConfig.getProxyPort();

    private static URI base;

    static {
        try {
            base = new URI("http://localhost:" + proxyPort + "/asbestos/proxy/default__default");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void nonZip() throws URISyntaxException, IOException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/"
                + "testrun/default__default/Test_Patients/Bob_Bangle?_gzip=false";

        HttpPost poster = new HttpPost();
        poster.postJson(new URI(url), null);
        assertEquals(200, poster.getStatus());

        Map<String, Map<String, String>> myMap = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertTrue(myMap.size() == 1);

        Map<String, String> testReport = myMap.get("Bob_Bangle");
        assertTrue(testReport.size() > 3);
        assertTrue(testReport.containsKey("resourceType"));
    }


    @Test
    void zip() throws URISyntaxException, IOException {
        String url = "http://localhost:" + proxyPort + "/asbestos/engine/"
                + "testrun/default__default/Test_Patients/Bob_Bangle?_gzip=true";

        HttpPost poster = new HttpPost();
        poster.postJson(new URI(url), null);
        assertEquals(200, poster.getStatus());

        Map<String, Map<String, String>> myMap = new Gson().fromJson(poster.getResponseText(), Map.class);
        assertTrue(myMap.size() == 1);

        Map<String, String> testReport = myMap.get("Bob_Bangle");
        assertTrue(testReport.size() > 3);
        assertTrue(testReport.containsKey("resourceType"));
    }

}
