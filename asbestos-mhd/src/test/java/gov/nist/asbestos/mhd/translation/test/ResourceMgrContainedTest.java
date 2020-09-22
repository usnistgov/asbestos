package gov.nist.asbestos.mhd.translation.test;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResolverConfig;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResourceMgrContainedTest {
    static ResourceWrapper docRef;
    ResourceMgr rMgr;
    Val val;


    @BeforeAll
    static void beforeAll() {
        InputStream is = ResourceMgrContainedTest.class.getResourceAsStream("/gov/nist/asbestos/mhd/translation/shared/DocumentReference1.xml");
        IBaseResource resource = ParserBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof DocumentReference);
        DocumentReference dr = (DocumentReference) resource;
        docRef = new ResourceWrapper(dr);
    }

    @BeforeEach
    void beforeEach() {
        rMgr = new ResourceMgr();
        val = new Val();
        rMgr.setVal(val);
    }

    @Test
    void contained() {
        Optional<ResourceWrapper> practitioner = rMgr.resolveReference(docRef, new Ref("#a4"), new ResolverConfig().containedRequired());
        assertTrue(practitioner.isPresent());
    }

    @Test
    void containedBadRef() {
        Optional<ResourceWrapper> practitioner = rMgr.resolveReference(docRef, new Ref("a4"), new ResolverConfig().containedRequired());
        assertFalse(practitioner.isPresent());
    }

    @Test
    void notContained() {
        Optional<ResourceWrapper> practitioner2 = rMgr.resolveReference(docRef, new Ref("#a444"), new ResolverConfig().containedRequired());
        assertFalse(practitioner2.isPresent());
    }
}
