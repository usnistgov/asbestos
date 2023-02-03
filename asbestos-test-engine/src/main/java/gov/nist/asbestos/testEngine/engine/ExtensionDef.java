package gov.nist.asbestos.testEngine.engine;

public class ExtensionDef {

    public static final String failure = "urn:failure";
    public static final String multiErrors = "urn:multipleErrors";
    public static final String noErrors = "urn:noErrors";

    public static final String expectFailure = "urn:asbestos:test:action:expectFailure";
    /**
     * coded as a FHIRPath collection string in (a|b...n) format
     */
    public static final String assertionIdList = "urn:asbestos:test:action:assertionIdList";
    public static final String mayHaveBugs = "urn:asbestos:test:action:mayHaveBugsWhichRequireManualReview";

    public static final String conditional = "urn:conditional";

    public static final String subFixture = "urn:subFixture";
    public static final String fhirPath = "urn:fhirPath";
    public static final String sourceId = "urn:sourceId";

    public static final String moduleId = "urn:moduleId";
    public static final String moduleName = "urn:moduleName";
    public static final String ts_import = "https://github.com/usnistgov/asbestos/wiki/TestScript-Import";

    public static final String ts_conditional = "https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional";
    public static final String component = "component";
    public static final String fixtureIn = "urn:fixture-in";

    public static final String fixtureOut = "urn:fixture-out";
    public static final String variableIn = "urn:variable-in";
    public static final String variableInNoTranslation = "urn:variable-in-no-translation";

    public static final String variableOut = "urn:variable-out";
    public static final String componentParameters = "urn:component-parameters";
}
