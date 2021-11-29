package gov.nist.asbestos.fixture;

/**
 * Search order is
 * Current Test Collection, Test Bundle directory
 * Current Test Collection, Common directory
 * Base Test Collection, Current Test Name, Bundle directory
 * Base Test Collection, Common directory
 * Base Test Collection, Base Test Name Bundle directory
 * Base Test Collection Common directory
 */
public enum FixturePlaceholderParamEnum {
    fixtureId,
    baseTestCollection,
    baseTestName
}
