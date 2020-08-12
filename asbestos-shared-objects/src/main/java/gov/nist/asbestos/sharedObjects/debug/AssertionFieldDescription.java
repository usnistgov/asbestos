package gov.nist.asbestos.sharedObjects.debug;

import java.util.List;

public class AssertionFieldDescription {
    private String name;
    private String formalDefinition;
    private List<AssertionFieldValueDescription> values;

    public AssertionFieldDescription(String name, String formalDefinition, List<AssertionFieldValueDescription> values) {
        this.name = name;
        this.formalDefinition = formalDefinition;
        this.values = values;
    }


    public String getName() {
        return name;
    }

    public String getFormalDefinition() {
        return formalDefinition;
    }

    public List<AssertionFieldValueDescription> getValues() {
        return values;
    }
}
