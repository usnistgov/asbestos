package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.asbestosProxySupport.Base.ProxyBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine {
    private File testDef = null;
    private URI sut = null;
    private TestScript testScript = null;

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut) {
        this.testDef = testDef;
        this.sut = sut;
    }

    public void run() {
        Objects.requireNonNull(testDef);
        Objects.requireNonNull(sut);
        doWorkflow();
    }

    void doWorkflow() {
        testScript = loadTestScript();
        doPreProcessing();
        doLoadFixtures();
        doAutoCreates();
        doSetup();
        doTest();
        doTearDown();
        doPostProcessing();
    }

    void doPreProcessing() {

    }

    void doLoadFixtures() {

    }

    void doAutoCreates() {

    }

    void doSetup() {

    }

    void doTest() {

    }

    void doTearDown() {

    }

    void doPostProcessing() {

    }

    private TestScript loadTestScript() {
        Objects.requireNonNull(testDef);
        File location = new File(testDef, "TestScript.xml");
        if (!location.exists() || !location.canRead() ) {
            location = new File(testDef, "TestScript.json");
            if (!location.exists() || !location.canRead() ) {
                throw new RuntimeException("Cannot load TestScript (.xml or .json) from " + testDef);
            }
        }
        InputStream is;
        try {
            is = new FileInputStream(location);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        IParser parser = (location.toString().endsWith("xml") ? ProxyBase.getFhirContext().newXmlParser() : ProxyBase.getFhirContext().newJsonParser());
        IBaseResource resource = parser.parseResource(is);
        assert resource instanceof TestScript;
        return (TestScript) resource;
    }
}
