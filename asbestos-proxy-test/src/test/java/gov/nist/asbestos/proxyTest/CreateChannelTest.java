package gov.nist.asbestos.proxyTest;

import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class CreateChannelTest {

    @Test
    void createAChannel() throws URISyntaxException, IOException {

        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir");
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), ChannelConfigFactory.convert(channelConfig));
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:8081/proxy/prox/default__test"));
        status = getter.getStatus();
        if (!(status == 200))
            fail("200 required - returned " + status);
        ChannelConfig returnConfig = ChannelConfigFactory.convert(getter.getResponseText());
        assertEquals(channelConfig, returnConfig);
    }

    @Test
    void deleteAndRecreateChannel() throws URISyntaxException, IOException {
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir");

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:8081/proxy/prox/default__test"));
        assertEquals(200, deleter.getStatus());

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:8081/proxy/prox/default__test"));
        assertEquals(404, getter.getStatus());

        // create - must return 201 (didn't exist)
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), json);
        assertEquals(201, poster.getStatus());

        // create - must return 200 (did exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), json);
        assertEquals(200, poster.getStatus());

    }

    @Test
    void createMultipleChannels() throws URISyntaxException, IOException {
        String channelConfig1 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test1")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir"));

        String channelConfig2 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test2")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir"));

        HttpDelete deleter;
        HttpPost poster;
        HttpGet getter;
        int status;

        deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:8081/proxy/prox/default__test1"));
        assertNotEquals(500, deleter.getStatus());

        deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:8081/proxy/prox/default__test2"));
        assertNotEquals(500, deleter.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), channelConfig1);
        assertEquals(201, poster.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), channelConfig2);
        assertEquals(201, poster.getStatus());

        // create - must return 200 (did exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), channelConfig1);
        assertEquals(200, poster.getStatus());

    }
}
