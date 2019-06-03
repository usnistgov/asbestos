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

import static org.junit.jupiter.api.Assertions.assertEquals;

class PassthroughTest {

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
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/proxy/prox"), json);
        assertEquals(200, poster.getStatus());

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:8081/proxy/prox/default__test"));
        assertEquals(200, getter.getStatus());
        ChannelConfig returnConfig = ChannelConfigFactory.convert(getter.getResponseText());
        assertEquals(channelConfig, returnConfig);

        // delete
       HttpDelete deleter = new HttpDelete();
       deleter.run(new URI("http://localhost:8081/proxy/prox/default__test"));
       assertEquals(200, deleter.getStatus());

       // verify
       getter = new HttpGet();
       getter.getJson(new URI("http://localhost:8081/proxy/prox/default__test"));
       assertEquals(404, getter.getStatus());
    }
}
