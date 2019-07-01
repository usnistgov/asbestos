package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
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
import java.util.Set;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  {
    private File testDef = null;
    private URI sut = null;
    private TestScript testScript = null;
    private Map<String, FixtureComponent> fixtures = new HashMap<>();
    private Val val;
    private ValE engineVal;
    private FhirClient fhirClient;

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
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClient = new FhirClient().setResourceCacheMgr(inTestResources);
    }

    public TestEngine run() {
        Objects.requireNonNull(val);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        doWorkflow();
        return this;
    }

    private void doWorkflow() {
        testScript = loadTestScript();
        doPreProcessing();
        doLoadFixtures();
        doAutoCreates();
        doSetup();
        doTest();
        doTearDown();
        doPostProcessing();
    }

    private void doPreProcessing() {

    }


    private void doLoadFixtures() {
        TestScript.SetupActionComponent c;
        c.
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
                FixtureComponent fixtureMgr;
                try {
                    fixtureMgr = new FixtureComponent(id, wrapper, fhirClient).setVal(fVal).load();
                } catch (Throwable e) {
                    fVal.add(new ValE(e.getMessage()).asError());
                    return;
                }
                if (!fixtureMgr.IsOk())
                    fVal.add(new ValE("Failed to load Fixture " + id).asError());
                fixtures.put(id, fixtureMgr);
            }
        }
    }

    private void doAutoCreates() {

    }

    private void doSetup() {
        if (testScript.hasSetup()) {
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            if (comp.hasAction()) {
                for (TestScript.SetupActionComponent aComp : comp.getAction()) {
                    FixtureComponent fixtureComponent = new Se
                }
                fixtureComponent = new SetupAction(fixtures, comp.get)
            }
        }
    }

    private void doTest() {
        if (testScript.hasTest()) {
            ValE fVal = new ValE(engineVal).setMsg("Tests");

            for (TestScript.TestScriptTestComponent comp : testScript.getTest()) {
                String id = comp.getId();
                if (id == null || id.equals("")) {
                    fVal.add(new ValE("Fixture has no id").asError());
                    return;
                }
                fVal.add(new ValE("Test " + id));


            }

        }

    }

    private void doTearDown() {

    }

    private void doPostProcessing() {

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

    private TestEngine addFixture(FixtureComponent fixtureMgr) {
        fixtures.put(fixtureMgr.getId(), fixtureMgr);
        return this;
    }

    private TestEngine addFixture(String id, ResourceWrapper resourceWrapper) {
        FixtureComponent fixtureMgr = new FixtureComponent(id, resourceWrapper, fhirClient);
        return addFixture(fixtureMgr);
    }

    Map<String, FixtureComponent> getFixtures() {
        return fixtures;
    }

    boolean isOk() {
        return fixturesOk();
    }

    private boolean fixturesOk() {
        for (FixtureComponent fixtureMgr : fixtures.values()) {
            if (!fixtureMgr.IsOk())
                return false;
        }
        return true;
    }

    TestEngine setVal(Val val) {
        this.val = val;
        return this;
    }
}
