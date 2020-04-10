package gov.nist.asbestos.sharedObjects;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.Session;

public class TestScriptDebugState {
    Object lock;
    AtomicBoolean resume;
    ConcurrentSkipListSet breakpointSet;
    Session session;

    public TestScriptDebugState(AtomicBoolean resume, ConcurrentSkipListSet breakpointSet, Session session) {
        this.lock = new Object();
        this.resume = resume;
        this.breakpointSet = breakpointSet;
        this.session = session;
    }

    public Object getLock() {
        return lock;
    }

    public AtomicBoolean getResume() {
        return resume;
    }

    public ConcurrentSkipListSet getBreakpointSet() {
        return breakpointSet;
    }

    public Session getSession() {
        return session;
    }
}
