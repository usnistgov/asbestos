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
     *
     */
    TestLevelDependsOnMapKeys,
    /**
     * This is an internal test collection which should be hidden from the user interface
     * true or false
     */
    Hidden
}
