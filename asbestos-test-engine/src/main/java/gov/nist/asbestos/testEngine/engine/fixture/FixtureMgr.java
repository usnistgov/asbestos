package gov.nist.asbestos.testEngine.engine.fixture;


import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FixtureMgr {
    private Map<String, FixtureComponent> fixtures = new HashMap<>();
    private String lastOp = null;  // last operation that created a fixture
    private String testCollectionId = null;
    private String testId = null;
    private FhirClient fhirClient = null;


    public FixtureMgr() {

    }

    public void putAll(Map<String, FixtureComponent> fixtures) {
        for (String key : fixtures.keySet()) {
            put(key, fixtures.get(key));
        }
    }

    public void put(String name, FixtureComponent component) {
        fixtures.put(name, component);
        lastOp = name;
        component.setFixtureMgr(this);
    }

    public FixtureComponent get(String name) {
        return fixtures.get(name);
    }

    public String getLastOp() {
        return lastOp;
    }

    public Collection<FixtureComponent> values() {
        return fixtures.values();
    }

    public boolean containsKey(String id) {
        return fixtures.containsKey(id);
    }

    public Set<String> keySet() {
        return fixtures.keySet();
    }

    public FixtureComponent add(String id) {
        FixtureComponent c = new FixtureComponent(id);
        put(id, c);
        c.setFixtureMgr(this);
        return c;
    }

    public FixtureComponent add(String id, ResourceWrapper resourceWrapper) {
        FixtureComponent c = new FixtureComponent();
        c.setFixtureMgr(this);
        c.setResource(resourceWrapper);
        c.setId(id);
        put(id, c);
        return c;
    }

    public FixtureMgr setTestCollectionId(String testCollectionId) {
        this.testCollectionId = testCollectionId;
        return this;
    }

    public FixtureMgr setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public String getTestCollectionId() {
        return testCollectionId;
    }

    public String getTestId() {
        return testId;
    }

    public FhirClient getFhirClient() {
        return fhirClient;
    }

    public FixtureMgr setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }
}
