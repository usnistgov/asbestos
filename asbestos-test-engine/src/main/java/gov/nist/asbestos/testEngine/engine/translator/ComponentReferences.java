package gov.nist.asbestos.testEngine.engine.translator;

import java.util.List;

public class ComponentReferences {
    private List<ComponentReference> refs;

    public ComponentReferences(List<ComponentReference> refs) {
        this.refs = refs;
    }

    public ComponentReference getCallerIn(String name) {
        for (ComponentReference c : refs) {
            for (Parameter p : c.getFixturesIn()) {
                if (p.getCallerName().equals(name))
                    return c;
            }
        }
        return null;
    }

    public ComponentReference getCallerOut(String name) {
        for (ComponentReference c : refs) {
            for (Parameter p : c.getFixturesOut()) {
                if (p.getCallerName().equals(name))
                    return c;
            }
        }
        return null;
    }

    public ComponentReference getLocalIn(String name) {
        for (ComponentReference c : refs) {
            for (Parameter p : c.getFixturesIn()) {
                if (p.getLocalName().equals(name))
                    return c;
            }
        }
        return null;
    }

    public ComponentReference getLocalOut(String name) {
        for (ComponentReference c : refs) {
            for (Parameter p : c.getFixturesOut()) {
                if (p.getLocalName().equals(name))
                    return c;
            }
        }
        return null;
    }

    public List<ComponentReference> getReferences() {
        return refs;
    }
}
