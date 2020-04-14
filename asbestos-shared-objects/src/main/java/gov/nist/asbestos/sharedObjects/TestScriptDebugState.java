package gov.nist.asbestos.sharedObjects;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.Session;

public class TestScriptDebugState {
    Object lock;
    AtomicBoolean resume;
    AtomicBoolean kill;
    String testScriptIndex; /* TestCollectionIndex + TestScriptIndex */
    ConcurrentSkipListSet breakpointSet;
    Session session;

    public TestScriptDebugState(Session session, String testScriptIndex, ConcurrentSkipListSet breakpointSet) {
        this.lock = new Object();
        this.testScriptIndex = testScriptIndex;
        this.resume = new AtomicBoolean();
        this.kill = new AtomicBoolean();
        this.breakpointSet = breakpointSet;
        this.session = session;
    }

    public String getTestScriptIndex() {
        return testScriptIndex;
    }

    public Object getLock() {
        return lock;
    }

    public AtomicBoolean getResume() {
        return resume;
    }

    public AtomicBoolean getKill() {
        return kill;
    }

    public ConcurrentSkipListSet getBreakpointSet() {
        return breakpointSet;
    }

    public Session getSession() {
        return session;
    }
}
