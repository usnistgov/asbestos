package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.client.channel.ChannelConfig;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

    @Test
    void expect500() throws URISyntaxException, IOException {
        Ref channelRef = ChannelsForTests.gen500();
        FhirClient fhirClient = new FhirClient();
        ResourceWrapper responseWrapper = fhirClient.writeResource(new DocumentReference(),  channelRef, Format.JSON, new Headers().withContentType(Format.JSON.getContentType()));
        int status = responseWrapper.getStatus();
        assertEquals(500, status, "URI is " + channelRef.toString());
    }
    public static URI getChannelBase(ChannelConfig channelConfig) {
        String fhirToolkitBase = "http://localhost:" +  ITConfig.getProxyPort() + "/asbestos";
        try {
            return new URI(fhirToolkitBase + "/proxy/" + channelConfig.asFullId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
