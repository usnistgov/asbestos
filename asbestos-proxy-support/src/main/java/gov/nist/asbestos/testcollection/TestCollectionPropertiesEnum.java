package gov.nist.asbestos.testcollection;

/**
 * An enumeration of Test Collection properties
 */
public enum TestCollectionPropertiesEnum {
    /**
     * Test Collection Id
     */
    Id,
    /**
     * SUT test type
     * server or client
     */
    TestType,
    /**
     *
     */
    Channel,
    /**
     * Profile or IG Name
     * Text is only used for channel test collection display indexing purposes.
     * Value may be a comma separated list.
     */
    FhirIgName,
    /**
     * The actual IG version to use for the $validate operation.
     * Only useful if FhirIgName is a single value.
     * Or the FhirIgVersion is optionally used with $validate operation, i.e., FhirIgVersion is the version of IG loaded on the validation server.
     * Can be Null or an empty string.
     */
    FhirIgVersion,
    /**
     * If defined, this value overrides the main service.properties fhirValidationServer property.
     * The fall back property is the fhirValidationServer service property.
     * The value is a Channel Id.
     */
    FhirValidationChannelId,
    /**
     * This is an internal test collection which should be hidden from the user interface
     * true or false
     */
    Hidden,
    /**
     * TestCollection.properties declares
     *          Collection Level
     *          DependsOn=TC/TestName (which means a specific test in test collection) or TC/ (with a trailing slash means all tests in the TC)
     *          TestLevelDependencies=TestName1,TestName2 (These are prefixed with the current test collection name
     *          in the Map Key and its retrieved values from the test.properties file are prefixed with current test collection.) TC is not allowed here.
     *
     */
    DependsOn,
    /**
     *          Test Level
     *          DependsOn=TestName (current test collection scope) or TC/TestName or TC/
     */
    TestLevelDependsOnMapKeys

    }
