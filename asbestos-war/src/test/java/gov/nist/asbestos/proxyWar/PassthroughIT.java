package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
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
        fhirPort = ITConfig.getFhirPort();
        proxyPort = ITConfig.getProxyPort();
    }

   // @Test
    void deleteChannelsTest() throws URISyntaxException, IOException {
        deleteChannels();
        assertEquals(404, new HttpGetter().getJson(new URI("http://localhost:" + fhirPort + "/asbestos/channel/default__fhirpass")).getStatus());
    }

    private void deleteChannels() {
        new HttpDelete().run("http://localhost:" + proxyPort + "/asbestos/channel/default__fhirpass");
        new HttpDelete().run("http://localhost:"  + proxyPort + "/asbestos/channel/default__test");
        new HttpDelete().run("http://localhost:" + proxyPort + "/asbestos/channel/default__abc");
    }

    @Test
    void createPatientDirectTest() {
        client = ctx.newRestfulGenericClient(ITConfig.getFhirBase());

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = createPatient(patient, client);
        assertNotNull(id);
    }

    //TODO add gzip version
    @Test
    void createPatientThroughProxyTest() throws IOException, URISyntaxException {
        String testSession = "default";
        String channelId = "default"; //"fhirpass";
        deleteChannels();
        String base = createChannel(testSession, channelId);
        client = ctx.newRestfulGenericClient(base);

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = createPatient(patient, client);
        assertNotNull(id);
    }

    /**
     *
     * @param patient
     * @return id
     */
    static String createPatient(Patient patient, IGenericClient client) {
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
                .setChannelName(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("fhir")
                .setFhirBase("http://localhost:" + fhirPort + "/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/asbestos/channel"), json);
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);
        //return "http://localhost:8080/fhir/fhir";
        return "http://localhost:" + proxyPort + "/asbestos/proxy/" + testSession + "__" + channelId;
    }

}
