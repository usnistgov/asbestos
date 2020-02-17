package gov.nist.asbestos.asbestosApi.impl;

import gov.nist.asbestos.asbestosApi.TestSession;

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
