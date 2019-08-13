package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
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
        assertEquals(404, new HttpGet().getJson(new URI("http://localhost:" + fhirPort + "/proxy/channel/default__fhirpass")).getStatus());
    }

    private void deleteChannels() {
        new HttpDelete().run("http://localhost:" + proxyPort + "/proxy/channel/default__fhirpass");
        new HttpDelete().run("http://localhost:"  + proxyPort + "/proxy/channel/default__test");
        new HttpDelete().run("http://localhost:" + proxyPort + "/proxy/channel/default__abc");
    }

    @Test
    void createPatientDirectTest() {
        client = ctx.newRestfulGenericClient(ITConfig.getFhirBase());

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = Support.createPatient(patient, client);
        assertNotNull(id);
    }

    //TODO add gzip version
    @Test
    void createPatientThroughProxyTest() throws IOException, URISyntaxException {
        String testSession = "default";
        String channelId = "fhirpass";
        deleteChannels();
        String base = createChannel(testSession, channelId);
        client = ctx.newRestfulGenericClient(base);

        Patient patient = new Patient();
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        String id = Support.createPatient(patient, client);
        assertNotNull(id);
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
        poster.postJson(new URI("http://localhost:" + proxyPort + "/proxy/channel"), json);
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);
        //return "http://localhost:8080/fhir/fhir";
        return "http://localhost:" + proxyPort + "/proxy/fhir/" + testSession + "__" + channelId;
    }

}
