package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CapabilityStatementIT {
    private static FhirContext ctx;
    private static IGenericClient client;
    private static String proxyPort;
    private static final String channelId = "mhdtest";

    @BeforeAll
    static void beforeAll() throws URISyntaxException, IOException {
        URI baseUri = new URI("/asbestos/proxy/default__" + channelId);
        ctx = FhirContext.forR4();
        proxyPort = ITConfig.getProxyPort();
        String proxyBase = String.format("http://localhost:%s%s", proxyPort, baseUri.toString());
        System.out.println(String.format("Using proxyBase: %s", proxyBase));
        client = ctx.newRestfulGenericClient(proxyBase);

        deleteAndRecreateMhdTestChannel();

        resetServiceProperties();
    }


    private static void deleteAndRecreateMhdTestChannel() throws URISyntaxException, IOException {
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("mhd")
                .setXdsSiteName("bogus__rr"); // XdsSiteName is not used for this test

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__" + channelId));
        // could be 200 or 404
        //assertEquals(200, deleter.getStatus(), deleter.getResponseHeaders().toString());

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__" + channelId));
        assertEquals(404, getter.getStatus());

        // create - must return 201 (didn't exist)
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel"), json);
        assertEquals(201, poster.getStatus(), poster.getResponseHeaders().toString());

        // create - must return 200 (did exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel"), json);
        assertEquals(200, poster.getStatus(), poster.getResponseHeaders().toString());
    }

    /**
     * This test executes the client API in search of a bogus patient for the purpose of testing the FHIR client behaviour towards silent-retrieval of the capability statement.
     */
    @Test
    void searchBogusPatient() {
        // Perform a search
        // FHIR Client 3.7.0 silently executes a GET metadata request
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

    @Test
    void getCapabilityStatementWithoutLoggingEvent() throws Exception {
        ServiceProperties.getInstance().setProperty(ServicePropertiesEnum.LOG_CS_METADATA_REQUEST.getKey(), "false");
        ServiceProperties.getInstance().save();

        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/proxy/default__" + channelId + "/metadata"));
        assertEquals(200, getter.getStatus());
        assert getter.getResponseHeaders().getHeaderValue("x-proxy-event") == null;
    }

    @Test
    void getCapabilityStatementWithLoggingEvent() throws Exception {
        ServiceProperties.getInstance().setProperty(ServicePropertiesEnum.LOG_CS_METADATA_REQUEST.getKey(), "true");
        ServiceProperties.getInstance().save();

        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/proxy/default__" + channelId + "/metadata"));
        assertEquals(200, getter.getStatus());
        String xProxyEvent = getter.getResponseHeaders().getHeaderValue("x-proxy-event");
        assert xProxyEvent != null;
        assert xProxyEvent.length() > 0 && xProxyEvent.contains("/asbestos/log/default/mhdtest/metadata");
    }


    /**
     * This test will only run when executed in Maven\Jetty IT test run mode because the service.properties lies in the server which hosts Asbestos. The JUNIT test-classess service.properties does not exist if it tired to read/write to it, hence an update to the actual property file is required through the Java system variable.
     * @throws Exception
     */
    private static void resetServiceProperties() throws URISyntaxException {
        String spFileString = ServiceProperties.getLocalSpFile(CapabilityStatementIT.class).toString();
        String targetString = "test-classes";
        String replaceString = "asbestos-war" + File.separator + "WEB-INF" + File.separator + "classes";

        if (spFileString.contains(targetString)) {
            File spFile = new File(spFileString.replace(targetString, replaceString));

            assert  spFile.exists();

            System.setProperty("SERVICE_PROPERTIES", spFile.toString());
        }
    }

}
