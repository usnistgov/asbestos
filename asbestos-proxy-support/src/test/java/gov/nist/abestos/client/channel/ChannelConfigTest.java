package gov.nist.abestos.client.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelConfigTest {
    static ChannelConfig config = new ChannelConfig();

    @BeforeAll
    static void beforeAll() {
        config.setChannelName("HYLAND");
        config.setTestSession("default");
        config.setEnvironment("default");
        config.setFhirBase("http://hyland26:17025/fhir/MHD/Bundle");
    }

    @Test
    void withPretty() throws URISyntaxException {
        URI req = new URI("/asbestos/proxy/default__HYLAND?_pretty=true&_format=json");
        URI result = config.translateEndpointToFhirBase(req);
        String correct = "http://hyland26:17025/fhir/MHD/Bundle?_pretty=true&_format=json";
        assertEquals(correct, result.toString());
    }
}
