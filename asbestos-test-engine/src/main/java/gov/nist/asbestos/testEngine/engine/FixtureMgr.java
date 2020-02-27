package gov.nist.asbestos.testEngine.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class FixtureMgr {
    private Map<String, FixtureComponent> fixtures = new HashMap<>();
    private String lastOp = null;  // last operation that created a fixture

    FixtureMgr() {

    }

    void put(String name, FixtureComponent component) {
        fixtures.put(name, component);
        lastOp = name;
    }

    public FixtureComponent get(String name) {
        return fixtures.get(name);
    }

    String getLastOp() {
        return lastOp;
    }

    Collection<FixtureComponent> values() {
        return fixtures.values();
    }

    boolean containsKey(String id) {
        return fixtures.containsKey(id);
    }

    public Set<String> keySet() {
        return fixtures.keySet();
    }
}
