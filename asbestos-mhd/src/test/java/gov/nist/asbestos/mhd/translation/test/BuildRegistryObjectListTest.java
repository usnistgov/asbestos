package gov.nist.asbestos.mhd.translation.test;

import gov.nist.asbestos.asbestosProxySupport.Base.Base;
import gov.nist.asbestos.mhd.resolver.ResourceCacheMgr;
import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslatorBuilder;
import gov.nist.asbestos.mhd.translation.BundleToRegistryObjectList;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValErrors;
import gov.nist.asbestos.simapi.validation.ValFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class BuildRegistryObjectListTest {
    private static Bundle bundle;
    private static Val val;
    private ResourceMgr rMgr;
    private static ResourceMgr bundleMgr;
    private static File externalCache;

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        externalCache = Paths.get(BuildRegistryObjectListTest.class.getResource("/external_cache/findme.txt").toURI()).getParent().toFile();
        InputStream is = ResourceMgrContainedTest.class.getResourceAsStream("/gov/nist/asbestos/mhd/translation/shared/bundle.xml");
        IBaseResource resource = Base.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        bundle = (Bundle) resource;
    }

    @BeforeEach
    void beforeEach() {
        rMgr = new ResourceMgr();
        val = new Val();
        rMgr.setVal(val);
        rMgr.setResourceCacheMgr(new ResourceCacheMgr(externalCache));
        bundleMgr = new ResourceMgr();
        bundleMgr.setVal(val);
        bundleMgr.setBundle(bundle);
    }

    @Test
    void build() {
        InputStream is2 = this.getClass().getResourceAsStream("/gov/nist/asbestos/mhd/translation/shared/theCodes.xml");
        CodeTranslator codeTranslator = CodeTranslatorBuilder.read(is2);

        rMgr.setBundle(bundle);
        rMgr.getResourceMgrConfig().internalOnly();

        BundleToRegistryObjectList xlate = new BundleToRegistryObjectList();
        xlate
                .setCodeTranslator(codeTranslator)
                .setResourceMgr(rMgr)
                .setAssigningAuthorities(AssigningAuthorities.allowAny())
                .setVal(val);

        RegistryObjectListType rol = xlate.buildRegistryObjectList();
        if (val.hasErrors())
            fail( ValFactory.toJson(new ValErrors(val)));
        else
            System.out.println(ValFactory.toJson(val));

        assertNotNull(rol);
    }


}
