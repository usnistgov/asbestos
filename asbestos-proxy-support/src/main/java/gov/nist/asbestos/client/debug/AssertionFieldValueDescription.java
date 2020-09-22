package gov.nist.asbestos.client.debug;


public class AssertionFieldValueDescription {
    private String displayName;
    private String codeValue;
    private String definition;

    public AssertionFieldValueDescription(String displayName, String codeValue, String definition) {
        this.displayName = displayName;
        this.codeValue = codeValue;
        this.definition = definition;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public String getDefinition() {
        return definition;
    }
}
