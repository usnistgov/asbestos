package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.client.resolver.SearchParms;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetStaticFixtureIT {

    @Test
    void getBundle() throws Exception {
        FhirClient fhirClient = new FhirClient();
        URI uri = new URI(ITConfig.getFhirToolkitBase() + "/engine/staticFixture/IT_Test_Support/StaticFixture");
        SearchParms searchParms = new SearchParms();
        searchParms.setParms("?url=Bundle/pdb.xml", true);
        Ref ref = new Ref(uri, "Bundle", searchParms);
        ResourceWrapper wrapper = fhirClient.readResource(ref, Format.JSON);
        assertTrue(wrapper.isOk(), "uri is " + uri);
        assertTrue(wrapper.getResource() instanceof Bundle, "resource is " + wrapper.getResourceType());
    }

    @Test
    void getResourceInBundle() throws URISyntaxException, UnsupportedEncodingException {
        FhirClient fhirClient = new FhirClient();
        URI uri = new URI(ITConfig.getFhirToolkitBase() + "/engine/staticFixture/IT_Test_Support/StaticFixture");
        SearchParms searchParms = new SearchParms();
        searchParms.setParms("?url=Bundle/pdb.xml;fhirPath=Bundle.entry[0]", true);
        Ref ref = new Ref(uri, "DocumentReference", searchParms);
        ResourceWrapper wrapper = fhirClient.readResource(ref, Format.JSON);
        assertTrue(wrapper.isOk(), "uri is " + "resource is " + wrapper.getResourceType());
        assertTrue(wrapper.getResource() instanceof DocumentReference);
    }

}
