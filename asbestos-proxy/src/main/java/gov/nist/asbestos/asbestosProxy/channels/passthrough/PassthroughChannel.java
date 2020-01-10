package gov.nist.asbestos.asbestosProxy.channels.passthrough;

import gov.nist.asbestos.asbestosProxy.channel.BaseChannel;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;


public class PassthroughChannel extends BaseChannel /*implements IBaseChannel*/ {
    private String serverBase;
    private String proxyBase;

    public PassthroughChannel() {
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

    public static void passHeaders(HttpBase requestIn, HttpBase requestOut) {
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
        transformResponseLocationHeader(responseIn, responseOut, proxyHostPort);
        // responseOut.setResponse(responseIn.getResponse());
        transformResponseBody(responseIn, responseOut);
        responseOut.setStatus(responseIn.getStatus());
    }

    private void transformResponseBody(HttpBase responseIn, HttpBase responseOut) {
        String oldBase = null;
        String newBase = null;
        byte[] rawResponse = responseIn.getResponse();
        Format format = Format.fromContentType(responseIn.getResponseHeaders().getContentType().getValue());
        BaseResource resource = ProxyBase.parse(rawResponse, format);
        if (resource instanceof Bundle) {
            boolean updated = false;
            Bundle bundle = (Bundle) resource;
            if (bundle.hasLink()) {
                Bundle.BundleLinkComponent linkComponent = bundle.getLink("self");
                if (linkComponent != null) {
                    if (linkComponent.hasUrl()) {
                        oldBase = linkComponent.getUrl();
                        newBase = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) + "/proxy/" + channelConfig.asFullId();
                        linkComponent.setUrl(newBase);
                        updated = true;
                    }
                }
            }
            for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                if (component.hasResponse()) {
                    Bundle.BundleEntryResponseComponent responseComponent = component.getResponse();
                    if (responseComponent.hasLocation()) {
                        String location = responseComponent.getLocation();
                        Ref locRef = new Ref(location);
                        if (!locRef.isRelative()) {
                            locRef.rebase(newBase);
                            responseComponent.setLocation(locRef.toString());
                            updated = true;
                        }
                    }
                }
            }
            if (updated)
                rawResponse = ProxyBase.encode(bundle, format).getBytes();
        }
        responseOut.setResponse(rawResponse);
    }

    private void transformResponseLocationHeader(HttpBase responseIn, HttpBase responseOut, String proxyHostPort) {
        Headers headers = responseIn.getResponseHeaders();
        Header loc = headers.get("Content-Location");
        Header loc2 = headers.get("Location");
        if (proxyBase != null) {
            URI path = headers.getPathInfo();
            if (path != null) {
                Ref ref = new Ref(path);
                ref = ref.rebase(proxyBase);
                headers.setPathInfo(ref.getUri());
            }

            if (loc != null) {
                Ref locRef = new Ref(loc.getValue());
                Ref ref = locRef.rebase(proxyBase).withHostPort(proxyHostPort);
                loc.setValue(ref.toString());
            }
            if (loc2 != null) {
                Ref ref = new Ref(loc2.getValue()).rebase(proxyBase).withHostPort(proxyHostPort);
                loc2.setValue(ref.toString());
            }
        }
        responseOut.setResponseHeaders(headers);
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
