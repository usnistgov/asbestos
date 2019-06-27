package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  {
    private File testDef = null;
    private URI sut = null;
    private TestScript testScript = null;
    private Map<String, FixtureMgr> fixtures = new HashMap<>();
    private ResourceCacheMgr inTestResources;
    private Val val;
    private ValE engineVal;

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut) {
        Objects.requireNonNull(testDef);
        Objects.requireNonNull(sut);
        this.testDef = testDef;
        this.sut = sut;
        inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
    }

    public TestEngine run() {
        Objects.requireNonNull(val);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        doWorkflow();
        return this;
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
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures");

            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                String id = comp.getId();
                if (id == null || id.equals("")) {
                    fVal.add(new ValE("Fixture has no id").asError());
                    return;
                }
                fVal.add(new ValE("Fixture " + id));
                ResourceWrapper wrapper = new ResourceWrapper(new Ref(comp.getResource().getReference()));
                FixtureMgr fixtureMgr;
                try {
                    fixtureMgr = new FixtureMgr(id, wrapper, inTestResources).load();
                } catch (Throwable e) {
                    fVal.add(new ValE(e.getMessage()).asError());
                    return;
                }
                fixtures.put(id, fixtureMgr);
            }
        }
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

    private boolean isFixtureDefined(String id) {
        return fixtures.containsKey(id);
    }

    private TestEngine addFixture(FixtureMgr fixtureMgr) {
        fixtures.put(fixtureMgr.getId(), fixtureMgr);
        return this;
    }

    private TestEngine addFixture(String id, ResourceWrapper resourceWrapper) {
        FixtureMgr fixtureMgr = new FixtureMgr(id, resourceWrapper, inTestResources);
        return addFixture(fixtureMgr);
    }

    public Map<String, FixtureMgr> getFixtures() {
        return fixtures;
    }

    public TestEngine setVal(Val val) {
        this.val = val;
        return this;
    }
}
