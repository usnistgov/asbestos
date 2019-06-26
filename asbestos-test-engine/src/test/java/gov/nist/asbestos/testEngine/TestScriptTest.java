package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.asbestosProxySupport.Base.ProxyBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestScriptTest {

    @Test
    void test() {
        InputStream is = TestScriptTest.class.getResourceAsStream("/testscript1.xml");
        IBaseResource resource = ProxyBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof TestScript);
        TestScript testScript = (TestScript) resource;

        if (testScript == null)
            fail("foo");
    }
}
