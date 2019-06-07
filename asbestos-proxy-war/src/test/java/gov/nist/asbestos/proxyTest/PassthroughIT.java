package gov.nist.asbestos.proxyTest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class PassthroughIT {
    private static FhirContext ctx;
    private IGenericClient client;
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        ctx = FhirContext.forR4();
        fhirPort = "8080"; //System.getProperty("fhir.port", "8080");
        proxyPort = "8081"; //System.getProperty("proxy.port", "8081");
    }

   // @Test
    void deleteChannelsTest() throws URISyntaxException, IOException {
        deleteChannels();
        assertEquals(404, new HttpGet().getJson(new URI("http://localhost:" + fhirPort + "/proxy/prox/default__fhirpass")).getStatus());
    }

    private void deleteChannels() {
        new HttpDelete().run("http://localhost:" + proxyPort + "/proxy/prox/default__fhirpass");
        new HttpDelete().run("http://localhost:"  + proxyPort + "/proxy/prox/default__test");
        new HttpDelete().run("http://localhost:" + proxyPort + "/proxy/prox/default__abc");
    }

    @Test
    void createPatientDirectTest() {
        client = ctx.newRestfulGenericClient("http://localhost:" + fhirPort + "/fhir/fhir");

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = createPatient(patient);
        assertNotNull(id);
    }

    @Test
    void createPatientThroughProxyTest() throws IOException, URISyntaxException {
        deleteChannels();
        String base = createChannel("default", "fhirpass");
        client = ctx.newRestfulGenericClient(base);

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = createPatient(patient);
        assertNotNull(id);
    }

    private String createPatient(Patient patient) {
        // Invoke the server create method (and send pretty-printed JSON
        // encoding to the server
        // instead of the default which is non-pretty printed XML)
        MethodOutcome outcome = client.create()
                .resource(patient)
                .prettyPrint()
                .encodedJson()
                .execute();

        // The MethodOutcome object will contain information about the
        // response from the server, including the ID of the created
        // resource, the OperationOutcome response, etc. (assuming that
        // any of these things were provided by the server! They may not
        // always be)
        IIdType id = (IIdType) outcome.getId();
        System.out.println("Got ID: " + id.getValue());
        return id.getValue();
    }

    private String createChannel(String testSession, String channelId) throws URISyntaxException, IOException {
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:" + fhirPort + "/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/proxy/prox"), json);
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);
        //return "http://localhost:8080/fhir/fhir";
        return "http://localhost:" + proxyPort + "/proxy/prox/" + testSession + "__" + channelId + "/Channel";
    }

}
