package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static groovy.util.GroovyTestCase.assertEquals;

class BundleIdsITx {
    static String bundleStr;
    static Bundle bundle;

    @BeforeAll
    static void beforeAll() {
        URL url = BundleIdsITx.class.getResource("/bundleIds/pdb.json");
        try {
            File bundleFile = Paths.get(BundleIdsITx.class.getResource("/bundleIds/pdb.json").toURI()).toFile();
            bundleStr = new String(Files.readAllBytes(Paths.get(bundleFile.toString())));
        } catch (Throwable t) {
            assert false;
        }

        bundle = (Bundle) ProxyBase.parse(bundleStr, Format.JSON);
    }

    @Test
    void sendIt() {
        String defaultProxyBase = "http://localhost:" + ITConfig.getProxyPort() + "/asbestos/proxy/default__default";
        ResourceWrapper response = new FhirClient().writeResource(
                bundle,
                new Ref(defaultProxyBase),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()).asMap());
        assertEquals(200, response.getStatus());

    }
}
