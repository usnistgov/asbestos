package gov.nist.asbestos.mhd.transactions.test;

import gov.nist.asbestos.asbestosProxySupport.Base.Base;
import gov.nist.asbestos.mhd.resolver.Ref;
import gov.nist.asbestos.mhd.resolver.ResolverConfig;
import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResourceManagerBundleTest {
    private static Bundle bundle;
    private static Val val;
    private ResourceMgr rMgr;
    private static ResourceMgr bundleMgr;

    @BeforeAll
    static void beforeAll() {
        InputStream is = ResourceMgrContainedTest.class.getResourceAsStream("/gov/nist/asbestos/mhd/transactions/shared/bundle.xml");
        IBaseResource resource = Base.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        bundle = (Bundle) resource;
    }

    @BeforeEach
    void beforeEach() {
        rMgr = new ResourceMgr();
        val = new Val();
        rMgr.setVal(val);
        bundleMgr = new ResourceMgr();
        bundleMgr.setVal(val);
        bundleMgr.setBundle(bundle);
    }

    @Test
    void absolute_noContaining_externalRequired() {
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(null, new Ref("http://myserver/Patient/1"), new ResolverConfig().externalRequired());
        if (val.hasErrors()) {
            fail(val.toString());
        }
        assertTrue(oResource.isPresent());
        assertNotNull(oResource.get().getUrl());
        assertFalse(oResource.get().isLoaded());
        assertEquals("http://myserver/Patient/1", oResource.get().getUrl().toString());
    }

    @Test
    void absolute_hasContaining_externalRequired() {
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(bundleMgr.getBundleResources().get(0), new Ref("http://myserver/Patient/1"), new ResolverConfig().externalRequired());
        if (val.hasErrors()) {
            fail(val.toString());
        }
        assertTrue(oResource.isPresent());
        assertNotNull(oResource.get().getUrl());
        assertFalse(oResource.get().isLoaded());
        assertEquals("http://myserver/Patient/1", oResource.get().getUrl().toString());
    }

    @Test
    void absolute_notAllowedInBundle() {
        rMgr.setBundle(bundle);
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(null,
                new Ref("http://localhost:9556/svc/fhir/Binary/1e404af3-077f-4bee-b7a6-a9be97e1ce32"),
                new ResolverConfig().externalRequired());
        assertFalse(oResource.isPresent());
    }

    @Test
    void relative() {
        ResourceWrapper containing = new ResourceWrapper();
        containing.setUrl(new Ref("http://myhost/fhir/Patient/23"));
        Ref reference = new Ref("Patient/12");
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(containing, reference,
                new ResolverConfig().relativeOk());
        if (val.hasErrors())
            fail(val.toString());
        assertTrue(oResource.isPresent());
        assertNotNull(oResource.get().getUrl());
        assertEquals("http://myhost/fhir/Patient/12", oResource.get().getUrl().toString());
        assertFalse(oResource.get().isLoaded());
    }

    @Test
    void relativeInBundle() {
        rMgr.setBundle(bundle);
        ResourceWrapper containing = new ResourceWrapper();
        containing.setUrl(new Ref("http://localhost:9556/svc/fhir/DocumentReference/45"));
        Ref reference = new Ref("Binary/1e404af3-077f-4bee-b7a6-a9be97e1ce32");
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(containing, reference,
                new ResolverConfig().relativeOk());
        if (val.hasErrors())
            fail(val.toString());
        assertTrue(oResource.isPresent());
        assertNotNull(oResource.get().getUrl());
        assertEquals("http://localhost:9556/svc/fhir/Binary/1e404af3-077f-4bee-b7a6-a9be97e1ce32", oResource.get().getUrl().toString());
        assertTrue(oResource.get().isLoaded());
    }

    @Test
    void absoluteInBundle() {
        rMgr.setBundle(bundle);
        Optional<ResourceWrapper> oResource = rMgr.resolveReference(null,
                new Ref("http://localhost:9556/svc/fhir/Binary/1e404af3-077f-4bee-b7a6-a9be97e1ce32"),
                new ResolverConfig().relativeOk());
        if (val.hasErrors())
            fail(val.toString());
        assertTrue(oResource.isPresent());
        assertNotNull(oResource.get().getUrl());
        assertEquals("http://localhost:9556/svc/fhir/Binary/1e404af3-077f-4bee-b7a6-a9be97e1ce32", oResource.get().getUrl().toString());
        assertTrue(oResource.get().isLoaded());
    }
}
