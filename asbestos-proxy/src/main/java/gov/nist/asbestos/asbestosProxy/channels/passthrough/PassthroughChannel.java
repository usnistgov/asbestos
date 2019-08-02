package gov.nist.asbestos.asbestosProxy.channels.passthrough;

import gov.nist.asbestos.asbestosProxy.channel.BaseChannel;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;


public class PassthroughChannel extends BaseChannel /*implements IBaseChannel*/ {
    private ChannelConfig channelConfig = null;
    private String serverBase;
    private String proxyBase;

    public PassthroughChannel() {
    }

    @Override
    public void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig;
    }

    @Override
    public void teardown() {

    }

    @Override
    public void validateConfig(ChannelConfig simConfig) {
    }

    @Override
    public void handle(Event event) {

    }

    private void passHeaders(HttpBase requestIn, HttpBase requestOut) {
        Headers inHeaders = requestIn.getRequestHeaders();
        Headers thruHeaders = inHeaders.select(Arrays.asList("content", "accept"));

        thruHeaders.setVerb(inHeaders.getVerb());
        thruHeaders.setPathInfo(inHeaders.getPathInfo());
        requestOut.setRequestHeaders(thruHeaders);
    }

    @Override
    public void transformRequest(HttpPost requestIn, HttpPost requestOut) {
        passHeaders(requestIn, requestOut);

        requestOut.setRequest(requestIn.getRequest());
    }

    @Override
    public void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        passHeaders(requestIn, requestOut);

        requestOut.setRequest(requestIn.getRequest());
    }

    @Override
    public void transformRequest(HttpDelete requestIn, HttpDelete requestOut) {
        passHeaders(requestIn, requestOut);

        requestOut.setRequest(requestIn.getRequest());
    }

    @Override
    public URI transformRequestUrl(String endpoint, HttpBase requestIn) {
        Objects.requireNonNull(channelConfig);
        try {
            return channelConfig.translateEndpointToFhirBase(requestIn.getRequestHeaders().getPathInfo());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort) {
        Headers headers = responseIn.getResponseHeaders();
        Header loc = headers.get("Content-Location");
        Header loc2 = headers.get("Location");
        if (proxyBase != null) {
            URI path = headers.getPathInfo();
            Ref ref = new Ref(path);
            ref = ref.rebase(proxyBase);
            headers.setPathInfo(ref.getUri());

            if (loc != null) {
                Ref locRef = new Ref(loc.getValue());
                ref = locRef.rebase(proxyBase).withHostPort(proxyHostPort);
                loc.setValue(ref.toString());
            }
            if (loc2 != null) {
                ref = new Ref(loc2.getValue()).rebase(proxyBase).withHostPort(proxyHostPort);
                loc2.setValue(ref.toString());
            }
        }
        responseOut.setResponseHeaders(headers);
        responseOut.setResponse(responseIn.getResponse());
    }

    @Override
    public void setServerBase(String serverBase) {
        this.serverBase = serverBase;
    }

    @Override
    public void setProxyBase(String proxyBase) {
        this.proxyBase = proxyBase;
    }
}
