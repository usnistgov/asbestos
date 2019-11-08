package gov.nist.asbestos.asbestosProxy.channels.capabilitystatement;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

class CapabilityStatementTest {

    @Test
    void isCapabilityStatementRequest() throws Exception {
            URI baseUri = new URI("/asbestos/proxy/default__mhdchannel");
            List<URI> goodMetadataRequestUri = Arrays.asList(
                    new URI("/asbestos/proxy/default__mhdchannel/metadata"),
                    new URI("/asbestos/proxy/default__mhdchannel/metadata?mode=full"));
            List<URI> badMetadataRequestUri = Arrays.asList(
                    new URI("/asbestos/proxy/default__mhdchannel/metadata/something"),
                    new URI("/asbestos/proxy/default__mhdchannel/somethingelse"),
                    new URI("/asbestos/proxy/default__mhdchannel/somethingelse/metadata"));

            Predicate<URI> uriPredicate = s -> FhirToolkitCapabilityStatement.isCapabilityStatementRequest(baseUri, s);

            goodMetadataRequestUri.forEach(s -> {assert uriPredicate.test(s);});
            badMetadataRequestUri.forEach(s -> {assert uriPredicate.negate().test(s);});
    }

    @Test
    void getCapabilityStatement() throws Exception {
        BaseResource baseResource = FhirToolkitCapabilityStatement.getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE);
        assert baseResource != null;
        assert baseResource instanceof CapabilityStatement;

        String content = ProxyBase.encode(baseResource, Format.XML);
        assert content.indexOf("${") == -1; // Parameters should have been replaced. If not check ServicePropertiesEnum to make sure the parameter have a matching key.
    }
}