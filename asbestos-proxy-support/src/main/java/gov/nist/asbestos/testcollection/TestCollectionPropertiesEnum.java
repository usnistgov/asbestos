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
     * Profile or IG Name (text may be something other than the exact IG name). Value may be a comma separated list.
     * This is mainly used for channel test collection indexing.
     */
    FhirIgName,
    /**
     * The actual IG version to use for the $validate operation.
     * Only useful if FhirIgName is a single value.
     */
    FhirIgVersion,
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
