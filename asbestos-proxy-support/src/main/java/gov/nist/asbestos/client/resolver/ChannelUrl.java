package gov.nist.asbestos.client.resolver;

import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// format of Channel URI is also found in ProxyServlet.parseUri()
public class ChannelUrl {
    private File externalCache;

    public ChannelUrl(File externalCache) {
        this.externalCache = externalCache;
    }

    private SimStore getSimStore(URI proxyUri) {
        SimId simId = getSimId(proxyUri);
        if (simId == null)
            return null;
        return new SimStore(externalCache, simId);
    }

    public URI getFhirBase(URI proxyUri) throws URISyntaxException {
        if (isProxyUri(proxyUri)) {
            SimStore simStore = getSimStore(proxyUri);
            if (simStore == null)
                return null;
            simStore.open();  // load config
            ChannelConfig channelConfig = simStore.getChannelConfig();
            if (channelConfig == null)
                return null;
            return new URI(channelConfig.getFhirBase());
        } else {
            return new Ref(proxyUri).getBase().getUri();
        }
    }

    private static boolean isProxyUri(URI uri) {
        List<String> uriParts = uriParts(uri);
        return uriParts.size() > 2 && uriParts.get(2).equals("proxy");
    }

    public static String getRelative(URI proxyUri) {
        List<String> uriParts = uriParts(proxyUri);
        uriParts.remove(0);  // leading empty string
        uriParts.remove(0);  // appContext
        uriParts.remove(0);  // proxy
        uriParts.remove(0);  // channelId
        return String.join("/", uriParts);
    }

    public static SimId getSimId(URI proxyUri) {
        List<String> uriParts = uriParts(proxyUri);
        if (uriParts.size() < 4)
            return null;
        return SimId.buildFromRawId(uriParts.get(3));
    }

    public static List<String> uriParts(URI uri) {
        Objects.requireNonNull(uri);
        List<String> uriParts1 = Arrays.asList(uri.getPath().split("/"));
        return new ArrayList<>(uriParts1);  // so parts are deletable
    }
}
