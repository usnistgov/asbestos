package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;

public class ChannelsForTests {
    public static ChannelConfig create(String testSession, String channelId, URI fhirBase) throws URISyntaxException, IOException {
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("fhir")
                .setFhirBase(fhirBase.toString());
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:" + ITConfig.getProxyPort() + "/asbestos/channel"), json);
        int status = poster.getStatus();
        if (!(status == 200 || status == 201))
            fail("200 or 201 required from CreateChannel - returned " + status);
        return channelConfig;
    }
}
