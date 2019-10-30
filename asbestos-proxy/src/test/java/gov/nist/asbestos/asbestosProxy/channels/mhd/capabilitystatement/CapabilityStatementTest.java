package gov.nist.asbestos.asbestosProxy.channels.mhd.capabilitystatement;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

class CapabilityStatementTest {

    @Test
    void isCapabilityStatementRequest() {
        try {
            URI baseUri = new URI("http://host:80/fhir");
            List<URI> goodMetadataRequestUri = Arrays.asList(
                    new URI("http://host:80/fhir/metadata"),
                    new URI("http://host:80/fhir/metadata?mode=full"));
            List<URI> badMetadataRequestUri = Arrays.asList(
                    new URI("http://host:80/fhir/metadata/something"),
                    new URI("http://host:80/fhir/somethingelse"));

            goodMetadataRequestUri.forEach(s -> {assert CapabilityStatement.isCapabilityStatementRequest(baseUri, s);});
            badMetadataRequestUri.forEach(s -> {assert ! CapabilityStatement.isCapabilityStatementRequest(baseUri, s);});

        } catch (URISyntaxException uriEx) {
            System.out.println(uriEx.toString());
        }
    }
}