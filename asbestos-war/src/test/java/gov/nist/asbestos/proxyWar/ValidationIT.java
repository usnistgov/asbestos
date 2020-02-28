package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValidationIT {
    static File ec;

    @BeforeAll
    static void beforeAll() {
        ec = new ExternalCache().getExternalCache();
    }

    @Test
    void patient() throws URISyntaxException, IOException {
        String validationServer = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        String patientStr = TestResource.get("/validation/patient.xml");
        BaseResource patient = ProxyBase.parse(patientStr, Format.XML);
        FhirClient fhirClient = new FhirClient();
        ResourceWrapper wrapper = fhirClient.writeResource(patient,
                        new Ref(validationServer + "/Patient/$validate?profile=http://hl7.org/fhir/StructureDefinition/Patient"),
                        Format.XML,
                        new Headers().withContentType(Format.XML.getContentType()));
        assertNotNull(wrapper.getResponseResource());
        if ("OperationOutcome".equals(wrapper.getResponseResource().getClass().getSimpleName())) {
            System.out.println(ProxyBase.encode(wrapper.getResponseResource(), Format.JSON));
        }
        assertEquals("OperationOutcome", wrapper.getResponseResource().getClass().getSimpleName());
    }
}
