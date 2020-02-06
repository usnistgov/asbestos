package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class Response500IT {
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        fhirPort = ITConfig.getFhirPort();
        proxyPort = ITConfig.getProxyPort();
    }

    void createChannel() throws URISyntaxException, IOException {
        Properties properties = System.getProperties();
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("g500")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8877/fhir/fhir");
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/asbestos/channel"), ChannelConfigFactory.convert(channelConfig));
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:" + proxyPort + "/asbestos/channel/default__test"));
        status = getter.getStatus();
        if (!(status == 200))
            fail("200 required - returned " + status);
        ChannelConfig returnConfig = ChannelConfigFactory.convert(getter.getResponseText());
        assertEquals(channelConfig, returnConfig);
    }

}
