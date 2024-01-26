package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.simapi.validation.*;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FixtureTest {

    private TestScript load(String path) {
        InputStream is = FixtureTest.class.getResourceAsStream(path);
        IBaseResource resource = ParserBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof TestScript);
        return (TestScript) resource;
    }

    @Test
    void simpleFixture() {
        TestScript testScript = load("/fixtures/simple/TestScript.xml");
        assertNotNull(testScript);
        assertEquals(1, testScript.getFixture().size());
        assertNotNull(testScript.getFixture().get(0).getResource());
        assertEquals("Patient/patient-example.xml", testScript.getFixture().get(0).getResource().getReference());
        assertFalse(testScript.getFixture().get(0).getAutocreate());
    }

//    @Test
//    void autoCreateFlag() {
//        TestScript testScript = load("/fixtures/autoCreate/script.xml");
//        assertNotNull(testScript);
//        assertEquals(1, testScript.getFixture().size());
//        assertTrue(testScript.getFixture().get(0).getAutocreate());
//    }
//
    @Test
    void loadFixtureFromTestDefinition() throws URISyntaxException {
        Val val = new Val();
        File testDef = Paths.get(getClass().getResource("/fixtures/fixtureFromTestDefinition/TestScript.xml").toURI()).getParent().toFile();
        URI sut = new URI("http://localhost:7080/fhir");



        File externalCache = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent().toFile();

        File test1 = Paths.get(getClass().getResource("/setup/write/createPatient/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, sut, null)
                .setTestSession(this.getClass().getSimpleName())
                .setChannelId(this.getClass().getSimpleName()+"__default")
                .setExternalCache(externalCache)
                .setVal(val)
                .runTest();

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
        if (val.hasWarnings())
            fail(ValFactory.toJson(new ValWarnings(val)));

        assertEquals(1, testEngine.getFixtures().keySet().size());
        FixtureComponent fixtureMgr = testEngine.getFixtures().values().iterator().next();
        IBaseResource resource = fixtureMgr.getResourceResource();
        assertNotNull(resource);
        assertTrue(resource instanceof Patient);
    }

    @Test
    void loadFixtureFromTestDefinitionBadReference() throws URISyntaxException {
        Val val = new Val();
        File testDef = Paths.get(getClass().getResource("/fixtures/fixtureFromBadTestDefinition/TestScript.xml").toURI()).getParent().toFile();
        URI sut = new URI("http://localhost:7080/fhir");
        File externalCache = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent().toFile();

        File test1 = Paths.get(getClass().getResource("/setup/write/createPatient/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, sut, null)
                .setTestSession(this.getClass().getSimpleName())
                .setChannelId(this.getClass().getSimpleName()+"__default")
                .setExternalCache(externalCache)
                .setVal(val)
                .runTest();

        System.out.println(testEngine.getTestReportAsJson());
        assertTrue(testEngine.hasError());
    }
}
