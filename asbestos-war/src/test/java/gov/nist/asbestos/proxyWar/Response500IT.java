package gov.nist.asbestos.proxyWar;

import com.google.gson.Gson;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
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
        URI fhirBase500 = new URI("http://localhost:" + ITConfig.getProxyPort()  + "/asbestos/gen500");
        ChannelConfig channelConfig = ChannelsForTests.create("default", "g500", fhirBase500);
        URI channelBase = getChannelBase(channelConfig);
        FhirClient fhirClient = new FhirClient();
        ResourceWrapper responseWrapper = fhirClient.writeResource(new DocumentReference(),  new Ref(channelBase), Format.JSON, new Headers().withContentType(Format.JSON.getContentType()));
        int status = responseWrapper.getStatus();
        assertEquals(500, status, "URI is " + fhirBase500);
    }
    public static URI getChannelBase(ChannelConfig channelConfig) {
        String fhirToolkitBase = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE);
        try {
            return new URI(fhirToolkitBase + "/proxy/" + channelConfig.asFullId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
