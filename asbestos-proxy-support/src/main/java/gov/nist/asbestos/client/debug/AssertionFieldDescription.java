package gov.nist.asbestos.client.debug;

import java.util.List;

public class AssertionFieldDescription {
    private String name;
    private String shortDefinition;
    private String formalDefinition;
    private List<AssertionFieldValueDescription> values;

    public AssertionFieldDescription(String name, String shortDefinition, String formalDefinition, List<AssertionFieldValueDescription> values) {
        this.name = name;
        this.shortDefinition = shortDefinition;
        this.formalDefinition = formalDefinition;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public String getShortDefinition() {
        return shortDefinition;
    }

    public String getFormalDefinition() {
        return formalDefinition;
    }

    public List<AssertionFieldValueDescription> getValues() {
        return values;
    }
}
