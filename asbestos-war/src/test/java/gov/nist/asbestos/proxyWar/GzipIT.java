package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import ihe.iti.xds_b._2007.DocumentRequest;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GzipIT {

    @Test
    void readZipContentEncodingDirect() {
        String fhirBase = "http://localhost:" + ITConfig.getFhirPort() + "/fhir/fhir";
        DocumentReference drReference = new DocumentReference()
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setDescription("Simple Document");

        FhirClient fhirClient = new FhirClient();
        ResourceWrapper write = fhirClient.writeResource(
                drReference,
                new Ref(fhirBase, "DocumentReference", null),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()).asMap());
        assertEquals(201, write.getStatus());

        ResourceWrapper readZipped = fhirClient.readResource(
                write.getRef(),
                new Headers()
                    .withAccept(Format.JSON.getContentType())
                        .withAcceptEncoding("gzip, deflate")
                        .asMap()
        );
        assertEquals(200, readZipped.getStatus());

        BaseResource resource = readZipped.getResource();
        assertTrue(resource instanceof DocumentReference);
    }

    @Test
    void readZipContentEncodingProxied() {
        String defaultProxyBase = "http://localhost:" + ITConfig.getProxyPort() + "/asbestos/proxy/default__default";

        DocumentReference drReference = new DocumentReference()
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setDescription("Simple Document");

        FhirClient fhirClient = new FhirClient();
        ResourceWrapper write = fhirClient.writeResource(
                drReference,
                new Ref(defaultProxyBase, "DocumentReference", null),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()).asMap());
        assertEquals(201, write.getStatus());

//        ResourceWrapper read = fhirClient.readResource(write.getRef(), new Headers().withAccept(Format.JSON.getContentType()).asMap());
//        assertEquals(200, read.getStatus());

        ResourceWrapper readZipped = fhirClient.readResource(
                write.getRef(),
                new Headers()
                        .withAccept(Format.JSON.getContentType())
                        .withAcceptEncoding("gzip, deflate")
                        .asMap()
        );
        assertEquals(200, readZipped.getStatus());

        BaseResource resource = readZipped.getResource();
        assertTrue(resource instanceof DocumentReference);
    }
}
