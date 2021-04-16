package gov.nist.asbestos.proxyWar;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.http.operations.HttpPut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
        String channelLocation = "http://localhost:" + proxyPort + "/asbestos/channel/default__test";

        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI(channelLocation));

        Properties properties = System.getProperties();
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelName("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:" + proxyPort + "/fhir/fhir");
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + proxyPort + "/asbestos/channel/create"), ChannelConfigFactory.convert(channelConfig));
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required - returned " + status);

        // verify
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI(channelLocation));
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
                .setChannelName("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:"+ proxyPort + "/fhir/fhir");

        String channelLocation = "http://localhost:"+ proxyPort + "/asbestos/channel/default__test";

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI(channelLocation));
        // A Get before the Create can be useful in checking for the assertion below
        // could be 200 or 404
        //assertEquals(200, deleter.getStatus(), deleter.getResponseHeaders().toString());

        // verify
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/default__test"));
        assertEquals(404, getter.getStatus());

        // create - must return 201 (didn't exist)
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/create"), json);
        assertEquals(201, poster.getStatus(), poster.getResponseHeaders().toString());

        // replace - must return 200 (did exist)
        HttpPut putter = new HttpPut();
        putter.putJson(new URI(channelLocation), json);
        assertEquals(200, putter.getStatus(), putter.getResponseHeaders().toString());

    }

    @Test
    void createMultipleChannels() throws URISyntaxException, IOException {
        String channelConfig1 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelName("test1")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8877/fhir/fhir"));

        String channelConfig2 = ChannelConfigFactory.convert(new ChannelConfig()
                .setTestSession("default")
                .setChannelName("test2")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8877/fhir/fhir"));

        HttpDelete deleter;
        HttpPost poster;
        HttpGetter getter;
        HttpPut putter;
        int status;

        String channel1Location = "http://localhost:"+ proxyPort + "/asbestos/channel/default__test1";
        String channel2Location = "http://localhost:"+ proxyPort + "/asbestos/channel/default__test2";

        deleter = new HttpDelete();
        deleter.run(new URI(channel1Location));
        assertNotEquals(500, deleter.getStatus());

        deleter = new HttpDelete();
        deleter.run(new URI(channel2Location));
        assertNotEquals(500, deleter.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/create"), channelConfig1);
        assertEquals(201, poster.getStatus());

        // create - must return 201 (didn't exist)
        poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/channel/create"), channelConfig2);
        assertEquals(201, poster.getStatus());

        // replace - must return 200 (did exist)
        putter = new HttpPut();
        putter.putJson(new URI(channel1Location), channelConfig1);
        assertEquals(200, putter.getStatus());

        getter = new HttpGetter();
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
