package gov.nist.abestos.client.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.Ref;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
        URI req = new URI( "/asbestos/proxy/default__HYLAND?_pretty=true&_format=json&patient.identifier=urn:oid:1.3.4%7CTEST-100");
        URI result = config.translateEndpointToFhirBase(req);
        URI correct = new URI("http://hyland26:17025/fhir/MHD/Bundle?_pretty=true&_format=json&patient.identifier=urn%3Aoid%3A1.3.4%7CTEST-100");

        List<String> resultParts = Arrays.asList(result.toString().split("\\?"));
        List<String> correctParts = Arrays.asList(correct.toString().split("\\?"));
        assertEquals(resultParts.get(0), correctParts.get(0));

        String resultPart1 = resultParts.get(1);
        String correctPart1 = correctParts.get(1);
        Map<String, String> resultMap = Ref.parseParameters(resultPart1);
        Map<String, String> correctMap = Ref.parseParameters(correctPart1);
        assertTrue(correctMap.size() == resultMap.size());

        boolean isNotSame = correctMap.entrySet()
                .stream()
                .anyMatch(e -> !resultMap.containsKey(e.getKey()) || !resultMap.get(e.getKey()).equals(e.getValue()));

        assertTrue(!isNotSame);
    }
}
