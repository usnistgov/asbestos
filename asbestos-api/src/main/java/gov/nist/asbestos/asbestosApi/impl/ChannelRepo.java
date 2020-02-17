package gov.nist.asbestos.asbestosApi.impl;

import gov.nist.asbestos.asbestosApi.Channel;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ChannelRepo {

    static public Channel get(String testSession, String channelId) throws Exception {
        String asbestosBase = ApiConfig.getAsbestosBase();
        HttpGet getter = new HttpGet();
        String url = asbestosBase + "/channel/" + testSession + "__" + channelId;
        getter.getJson(url);
        if (getter.getStatus() != 200)
            return null;
        String json = getter.getResponseText();
        return toChannel(ChannelConfigFactory.convert(json));
    }

    static private Channel toChannel(ChannelConfig config) {
        return new ChannelImpl(
                config.getEnvironment(),
                config.getTestSession(),
                config.getChannelId(),
                config.getChannelType()
        );
    }

    static public Channel create(String environment, String testSession, String channelType, String channelId) throws IOException, URISyntaxException {
        ChannelConfig config = new ChannelConfig()
                .setEnvironment(environment)
                .setTestSession(testSession)
                .setChannelType(channelType)
                .setChannelId(channelId);
        String json = ChannelConfigFactory.convert(config);
        HttpPost poster = new HttpPost();
        URI uri = new URI(ApiConfig.getAsbestosBase() + "/channel");
        poster.postJson(uri, json);
        if (poster.getStatus() == 200 || poster.getStatus() == 201)
            return toChannel(config);
        return null;
    }

    static public boolean delete(String testSession, String channelId) throws URISyntaxException {
        HttpDelete deleter = new HttpDelete().run(new URI(ApiConfig.getAsbestosBase() + "/channel/" + testSession + "__" + channelId));
        return deleter.isSuccess();
    }

    static public boolean delete(Channel channel) throws URISyntaxException {
        return delete(channel.getTestSession(), channel.getChannelId());
    }
}
