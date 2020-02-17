package gov.nist.asbestos.asbestosApi.repo;

import gov.nist.asbestos.asbestosApi.TestSession;

public interface TestSessionRepo {
    TestSession get(String testSessionName);
}
