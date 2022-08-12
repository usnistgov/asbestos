package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.operations.HttpPut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CapabilityStatementIT {
    private static FhirContext ctx;
    private static IGenericClient client;
    private static String proxyPort;
    private static final String channelIdWithCsLog = "mhdtest_with_cslog";
    private static final String channelIdWithoutCsLog = "mhdtest_without_cslog";


    @BeforeAll
    static void beforeAll() throws Exception {
        URI baseUri = new URI("/asbestos/proxy/default__" + channelIdWithCsLog);
        ctx = FhirContext.forR4();
        proxyPort = ITConfig.getProxyPort();
        String proxyBase = String.format("http://localhost:%s%s", proxyPort, baseUri.toString());
        System.out.println(String.format("Using proxyBase: %s", proxyBase));
        client = ctx.newRestfulGenericClient(proxyBase);

        deleteAndRecreateMhdTestChannel(channelIdWithCsLog, true);
        deleteAndRecreateMhdTestChannel(channelIdWithoutCsLog, false);
    }


    private static void deleteAndRecreateMhdTestChannel(String channelName, boolean csloggingEnabled) throws URISyntaxException, IOException {
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelName(channelName)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("mhd")
                .setXdsSiteName("bogus__rr")
                .setLogMhdCapabilityStatementRequest(csloggingEnabled); // XdsSiteName is not used for this test

        String channelLocation = "http://localhost:"+ proxyPort + "/asbestos/rw/channel/default__" + channelName;

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI( channelLocation ));
        // could be 200 or 404
        //assertEquals(200, deleter.getStatus(), deleter.getResponseHeaders().toString());

        // verify
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI(channelLocation));
        assertEquals(404, getter.getStatus());

        // create - must return 201 (didn't exist)
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/rw/channel/create"), json);
        assertEquals(201, poster.getStatus(), poster.getResponseHeaders().toString());

        // put - must return 200 (did exist)
        HttpPut putter = new HttpPut();
        putter.putJson(new URI(channelLocation), json);
        assertEquals(200, putter.getStatus(), putter.getResponseHeaders().toString());
    }

    /**
     * Executes the client API in search of a bogus patient for the purpose of testing the FHIR client behaviour towards silent-retrieval of the capability statement.
     */
    /*
    @Ignore
    void searchBogusPatient() {
        // Perform a search
        // FHIR Client 3.7.0 silently executes a GET metadata request.
        // To disable this:
        // Disable server validation (don't pull the server's metadata first)
        // ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        // For more details see https://hapifhir.io/doc_rest_client_http_config.html
        Bundle results = client
                .search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value("duck"))
                .returnBundle(Bundle.class)
                .execute();

        System.out.println("Found " + results.getEntry().size() + " entry(ies) in Result.");

        Resource r = results.getEntry().get(0).getResource();
        boolean isInstanceDetected = r instanceof OperationOutcome;
        System.out.println("Is instance type detected? " + isInstanceDetected);

        if (isInstanceDetected) {
           OperationOutcome outcome = (OperationOutcome)r;
           if (outcome.hasIssue()) {
               System.out.println(outcome.getIssueFirstRep().getSeverity().toString());
           }
        }
    }

     */

    @Test
    void getCapabilityStatementWithoutLoggingEvent() throws Exception {
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/proxy/default__" + channelIdWithoutCsLog + "/metadata"));
        assertEquals(200, getter.getStatus());
        assert getter.getResponseHeaders().getHeaderValue("x-proxy-event") == null;
    }

    @Test
    void getCapabilityStatementWithLoggingEvent() throws Exception {
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/proxy/default__" + channelIdWithCsLog + "/metadata"));
        assertEquals(200, getter.getStatus());
        String xProxyEvent = getter.getResponseHeaders().getHeaderValue("x-proxy-event");
        assert xProxyEvent != null;
        assert xProxyEvent.length() > 0 && xProxyEvent.contains("/asbestos/log/default/"+ channelIdWithCsLog +"/metadata");

        getter.getJson(new URI(xProxyEvent));
        assertEquals(200, getter.getStatus());
        assertNotNull(getter.getResponseText());
        String contentType = getter.getResponseHeaders().getHeaderValue("content-type");
        assert getter.getResponseText().length() > 0;
        assertNotNull(contentType);
        assert "application/json".equals(contentType);

        getter.get(new URI(xProxyEvent), "text/html");
        assertEquals(200, getter.getStatus());
        assertNotNull(getter.getResponseText());
        assert getter.getResponseText().length() > 0;

//        System.out.println(getter.getResponseText());
//        System.out.println(getter.getResponseHeaders().toString());
//        contentType = getter.getResponseHeaders().getHeaderValue("content-type");
//        assertNotNull(contentType);
//        assert "text/html".equals(contentType);
    }

}
