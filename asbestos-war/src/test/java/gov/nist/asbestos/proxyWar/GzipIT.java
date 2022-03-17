package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.util.Gzip;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static gov.nist.asbestos.proxyWar.Utility.proxyPort;
import static org.junit.jupiter.api.Assertions.*;

class GzipIT {
    @BeforeAll
    @Test
    static void deleteAndRecreateChannel() throws URISyntaxException, IOException {
        // create
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelName("forgzipit")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("fhir") // in many places, this is incorrectly coded as "passthrough". ProxyServlet will not be able find a ChannelBuilder for the 'passthrough' type.
                .setFhirBase(ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.HAPI_FHIR_BASE)
                );

        String channelLocation = "http://localhost:"+ proxyPort + "/asbestos/rw/channel/default__forgzipit";

        // delete
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpDelete deleter = new HttpDelete();
        deleter.run(new URI(channelLocation));
        // A Get before the Create can be useful in checking for the assertion below
        // could be 200 or 404
        //assertEquals(200, deleter.getStatus(), deleter.getResponseHeaders().toString());

        // verify
        HttpGetter getter = new HttpGetter();
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/rw/channel/default__forgzipit"));
        assertEquals(404, getter.getStatus());

        // create - must return 201 (didn't exist)
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:"+ proxyPort + "/asbestos/rw/channel/create"), json);
        assertEquals(201, poster.getStatus(), poster.getResponseHeaders().toString());

        // verify new channel exists
        getter.getJson(new URI("http://localhost:"+ proxyPort + "/asbestos/rw/channel/default__forgzipit"));
        assertEquals(200, getter.getStatus());

    }


    @Test
    void readZipContentEncodingDirect() {
        String fhirBase = "http://localhost:" + ITConfig.getFhirPort() + "/fhir";
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
        String defaultProxyBase = "http://localhost:" + ITConfig.getProxyPort() + "/asbestos/proxy/default__forgzipit";

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
        String fhirBase = "http://localhost:" + ITConfig.getFhirPort() + "/fhir";
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
                System.out.println(ParserBase.encode(oo, Format.JSON));
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
        String fhirBase = "http://localhost:" + ITConfig.getProxyPort() + "/asbestos/proxy/default__forgzipit";
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
                System.out.println(ParserBase.encode(oo, Format.JSON));
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

