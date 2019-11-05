package gov.nist.asbestos.asbestosProxy.channels.mhd.capabilitystatement;

import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
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

            Predicate<URI> uriPredicate = s -> CapabilityStatement.isCapabilityStatementRequest(baseUri, s);

            goodMetadataRequestUri.forEach(s -> {assert uriPredicate.test(s);});
            badMetadataRequestUri.forEach(s -> {assert uriPredicate.negate().test(s);});
    }

    @Test
    void getCapabilityStatement() throws Exception {
        BaseResource baseResource = CapabilityStatement.getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE);
        assert baseResource != null;

        // transform to json and back to xml as a test? (ProxyBase.encode(baseResource, Format.JSON));
    }
}