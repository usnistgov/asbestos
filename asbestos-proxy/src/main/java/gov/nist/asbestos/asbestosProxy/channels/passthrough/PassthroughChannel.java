package gov.nist.asbestos.asbestosProxy.channels.passthrough;

import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.asbestosProxy.channel.IBaseChannel;
import gov.nist.asbestos.asbestosProxy.events.EventStore;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;


public class PassthroughChannel implements IBaseChannel {
    private ChannelConfig channelConfig = null;

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
    public void handle(EventStore event) {

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
    public void transformResponse(HttpBase responseIn, HttpBase responseOut) {
        responseOut.setResponseHeaders(responseIn.getResponseHeaders());
        responseOut.setResponse(responseIn.getResponse());
    }
}
