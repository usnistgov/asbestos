package gov.nist.asbestos.mhd.translation.test;

import gov.nist.asbestos.asbestosProxySupport.Base.ProxyBase;
import gov.nist.asbestos.asbestosProxySupport.resolver.ResourceMgr;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceManagerBundleWithDupTest {
    private static Bundle bundle;
    private static Val val;
    private ResourceMgr rMgr;

    @BeforeAll
    static void beforeAll() {
        InputStream is = ResourceMgrContainedTest.class.getResourceAsStream("/gov/nist/asbestos/mhd/translation/shared/bundleWithDuplicate.xml");
        IBaseResource resource = ProxyBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        bundle = (Bundle) resource;
    }

    @BeforeEach
    void beforeEach() {
        rMgr = new ResourceMgr();
        val = new Val();
        rMgr.setVal(val);
    }

    @Test
    void duplicateUrlInBundle() {
        rMgr.setBundle(bundle);
        assertTrue(val.hasErrors());
    }

}
