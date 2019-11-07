package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

}
