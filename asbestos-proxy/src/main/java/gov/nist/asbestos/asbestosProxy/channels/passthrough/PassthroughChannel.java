package gov.nist.asbestos.asbestosProxy.channels.passthrough;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.channel.BaseChannel;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.general.ChannelSupport;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;


public class PassthroughChannel extends BaseChannel /*implements IBaseChannel*/ {
    private String serverBase;
    private String proxyBase;

    public PassthroughChannel(ChannelConfig simConfig) {
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

    @Override
    public void transformRequest(HttpPost requestIn, HttpPost requestOut) {
        ChannelSupport.passHeaders(requestIn, requestOut);

        requestOut.setRequest(requestIn.getRequest());
    }

    @Override
    public void transformRequest(HttpGetter requestIn, HttpGetter requestOut) {
        ChannelSupport.passHeaders(requestIn, requestOut);

        requestOut.setRequest(requestIn.getRequest());
    }

    @Override
    public void transformRequest(HttpDelete requestIn, HttpDelete requestOut) {
        ChannelSupport.passHeaders(requestIn, requestOut);

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
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort, String requestedType, String search) {
        transformResponseLocationHeader(responseIn, responseOut, proxyHostPort);
        transformResponseBody(responseIn, responseOut);
        responseOut.setStatus(responseIn.getStatus());
    }

    private String getResource(URI uri) {
        try {
            String uriString = uri.toString();
            String pathSeparator = "/";
            String path = uriString.replace(channelConfig.getFhirBase() + pathSeparator, "");
            String parts[] = path.split(pathSeparator);
            return parts[0]; // The resource
        } catch (Exception ex) {
            return null;
        }
    }

    private void transformResponseBody(HttpBase responseIn, HttpBase responseOut) {
        String newBase;
        byte[] rawResponse = responseIn.getResponse();
        if (rawResponse == null) {
            responseOut.setStatus(responseIn.getStatus());
            return;
        }

       String requestedResource = getResource(responseIn.getUri());
        if (requestedResource != null && ! "Binary".equals(requestedResource)) {
            Format format = Format.fromContentType(responseIn.getResponseHeaders().getContentType().getValue());

            BaseResource resource = ParserBase.parse(responseIn.getResponseText(), format);
            if (resource instanceof Bundle) {
                Bundle bundle = (Bundle) resource;
                boolean updated = false;
                Optional<String> externalPatientBase = ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.CAT_EXTERNAL_PATIENT_SERVER_FHIR_BASE);
                if (! (externalPatientBase.isPresent() && externalPatientBase.get().equals(channelConfig.getFhirBase()))) {
                    newBase = // ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_BASE)
//                        + "/proxy/" + channelConfig.asFullId();
                            channelConfig.getProxyURI().toString();
                    if (bundle.hasLink()) {
                        Bundle.BundleLinkComponent linkComponent = bundle.getLink("self");
                        if (linkComponent != null) {
                            if (linkComponent.hasUrl()) {
                                linkComponent.setUrl(new Ref(linkComponent.getUrl()).rebase(newBase).toString());
                                //linkComponent.setUrl(newBase);
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
                        if (component.hasFullUrl()) {
                            Ref fullUrl = new Ref(component.getFullUrl());
                            if (fullUrl.isAbsolute()) {
                                component.setFullUrl(fullUrl.rebase(newBase).toString());
                            }
                        }
                    }
                }
                if (updated) {
                    rawResponse = ParserBase.encode(bundle, format).getBytes();
                }
            }
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
                Ref ref = locRef.rebase(proxyBase).withHostPort(channelConfig.getScheme(), proxyHostPort);
                loc.setValue(ref.toString());
            }
            if (loc2 != null) {
                Ref ref = new Ref(loc2.getValue()).rebase(proxyBase).withHostPort(channelConfig.getScheme(), proxyHostPort);
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
