package gov.nist.asbestos.api.repo;

import gov.nist.asbestos.api.Test;
import gov.nist.asbestos.api.TestCollection;

public interface TestRepo {
    TestCollection getTestCollection(String testCollectionName);
    Test getTest(String testCollectionName, String testName);
}
