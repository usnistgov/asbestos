package gov.nist.asbestos.api.impl;

import gov.nist.asbestos.api.TestSession;

public class TestSessionImpl implements TestSession {
    private String name;

    public TestSessionImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
