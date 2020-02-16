package gov.nist.asbestos.proxyWar.basic;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.util.Gzip;
import gov.nist.asbestos.proxyWar.support.ITConfig;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void zipUnzip() {
        String data = "ABCDEFG";
        byte[] compressed = Gzip.compressGZIP(data.getBytes());
        String uncompressed = Gzip.decompressGZIPToString(compressed);
        assertEquals(data, uncompressed);
    }

    @Test
    void writeZipDirect() {
        String fhirBase = "http://localhost:" + ITConfig.getFhirPort() + "/fhir/fhir";
        DocumentReference drReference = new DocumentReference()
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setDescription("Simple Document");

        FhirClient fhirClient = new FhirClient().requestGzip(true).sendGzip(true);
        ResourceWrapper write = fhirClient.writeResource(
                drReference,
                new Ref(fhirBase, "DocumentReference", null),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()).asMap());

        Headers requestHeaders = write.getHttpBase().getRequestHeaders();

        assertNotNull(requestHeaders.get("Content-Encoding"));

        if (write.getStatus() >= 400) {
            BaseResource result = write.getResource();
            if (result instanceof OperationOutcome) {
                OperationOutcome oo = (OperationOutcome) result;
                System.out.println(ProxyBase.encode(oo, Format.JSON));
                assert false;
            }
        }

        assertEquals(201, write.getStatus());

        ResourceWrapper readZipped = fhirClient.readResource(
                write.getRef(),
                new Headers()
                        .withAccept(Format.JSON.getContentType())
                       // .withAcceptEncoding("gzip, deflate")
                        .asMap()
        );
        assertEquals(200, readZipped.getStatus());

        BaseResource resource = readZipped.getResource();
        assertTrue(resource instanceof DocumentReference);
    }

    @Test
    void writeZipProxy() {
        String fhirBase = "http://localhost:" + ITConfig.getProxyPort() + "/asbestos/proxy/default__default";
        DocumentReference drReference = new DocumentReference()
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setDescription("Simple Document");

        FhirClient fhirClient = new FhirClient().requestGzip(true).sendGzip(true);
        ResourceWrapper write = fhirClient.writeResource(
                drReference,
                new Ref(fhirBase, "DocumentReference", null),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()).asMap());

        Headers requestHeaders = write.getHttpBase().getRequestHeaders();

        assertNotNull(requestHeaders.get("Content-Encoding"));

        if (write.getStatus() >= 400) {
            BaseResource result = write.getResource();
            if (result instanceof OperationOutcome) {
                OperationOutcome oo = (OperationOutcome) result;
                System.out.println(ProxyBase.encode(oo, Format.JSON));
                assert false;
            }
        }

        assertEquals(201, write.getStatus());

        ResourceWrapper readZipped = fhirClient.readResource(
                write.getRef(),
                new Headers()
                        .withAccept(Format.JSON.getContentType())
                        // .withAcceptEncoding("gzip, deflate")
                        .asMap()
        );
        assertEquals(200, readZipped.getStatus());

        BaseResource resource = readZipped.getResource();
        assertTrue(resource instanceof DocumentReference);
    }
}

