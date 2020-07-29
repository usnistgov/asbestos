package gov.nist.asbestos.proxyWar;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CreateChannelIT {
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        fhirPort = ITConfig.getFhirPort();
        proxyPort = ITConfig.getProxyPort();
    }

    @Test
    void createAChannel() throws URISyntaxException, IOException {
        Properties properties = System.getProperties();
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:" + proxyPort + "/fhir/fhir");
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

    @Test
    void deleteAndRecreateChannel() throws URISyntaxException, IOException {
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:"+ proxyPort + "/fhir/fhir");

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__test"));
        // could be 200 or 404
        //assertEquals(200, deleter.getStatus(), deleter.getResponseHeaders().toString());

        // verify
        HttpGet getter = new HttpGet();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__test"));
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

    @Test
    void createMultipleChannels() throws URISyntaxException, IOException {
        String channelConfig1 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test1")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8877/fhir/fhir"));

        String channelConfig2 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test2")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8877/fhir/fhir"));

        HttpDelete deleter;
        HttpPost poster;
        HttpGet getter;
        int status;

        deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__test1"));
        assertNotEquals(500, deleter.getStatus());

        deleter = new HttpDelete();
        deleter.run(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__test2"));
        assertNotEquals(500, deleter.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel"), channelConfig1);
        assertEquals(201, poster.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel"), channelConfig2);
        assertEquals(201, poster.getStatus());

        // create - must return 200 (did exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel"), channelConfig1);
        assertEquals(200, poster.getStatus());

        getter = new HttpGet();
        getter.get("http://localhost:"+ proxyPort + "/asbestos/channel");
        assertEquals(200, getter.getStatus());
        String response = getter.getResponseText();

        Type stringListType = new TypeToken<ArrayList<String>>(){}.getType();
        List<String> aList = new Gson().fromJson(response, stringListType);

        assertTrue(aList.contains("default__test1"));
        assertTrue(aList.contains("default__test2"));
    }

    private static List<String> deleteEmpty(List<String> list) {
        List<String> result = new ArrayList<>();

        for (String item : list) {
            if (item.equals(""))
                continue;
            result.add(item);
        }

        return result;
    }
}
