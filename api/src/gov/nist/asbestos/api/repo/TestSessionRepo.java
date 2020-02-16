package gov.nist.asbestos.api.repo;

import gov.nist.asbestos.api.TestSession;

public interface TestSessionRepo {
    TestSession get(String testSessionName);
}
