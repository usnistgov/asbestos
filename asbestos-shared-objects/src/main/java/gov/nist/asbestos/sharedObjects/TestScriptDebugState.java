package gov.nist.asbestos.sharedObjects;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.Session;

public class TestScriptDebugState {
    Object lock;
    AtomicBoolean resume;
    AtomicBoolean kill;
    AtomicBoolean evaluateMode;
    String testScriptIndex; /* TestCollectionIndex + TestScriptIndex */
    ConcurrentSkipListSet breakpointSet;
    Session session;
    String userType;
    String evalJsonString;

    private static Logger log = Logger.getLogger(TestScriptDebugState.class);


    public TestScriptDebugState(Session session, String userType, String testScriptIndex, ConcurrentSkipListSet breakpointSet) {
        this.lock = new Object();
        this.testScriptIndex = testScriptIndex;
        this.resume = new AtomicBoolean();
        this.kill = new AtomicBoolean();
        this.evaluateMode = new AtomicBoolean();
        this.breakpointSet = breakpointSet;
        this.session = session;
        this.userType = userType;
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

    public AtomicBoolean getEvaluateMode() {
        return evaluateMode;
    }

    public String getEvalJsonString() {
        return evalJsonString;
    }

    public void setEvalJsonString(String evalJsonString) {
        this.evalJsonString = evalJsonString;
    }

    /**
     * Reset Resume: will resume the normal program flow until the next breakpoint
     */
    public void cancelResumeMode() {
       this.resume.set(false);
    }

    /**
     * Reset Eval mode: will loop and wait for Resume or Eval
     */
    public void cancelEvalMode() {
       this.evaluateMode.set(false);
    }

    public void sendKilled() {
        getSession().getAsyncRemote().sendText("{\"messageType\":\"killed\", \"testReport\":{}}");
    }

    public void sendUnexpectedError() {
        getSession().getAsyncRemote().sendText("{\"messageType\":\"unexpected-error\", \"testReport\":{}}");
    }

    public void sendFinalReport(String testReport) {
        getSession().getAsyncRemote().sendText("{\"messageType\":\"final-report\", \"testReport\":" + testReport +"}");
    }

    public void sendAssertionStr(String assertionJson) {
        getSession().getAsyncRemote().sendText("{\"messageType\":\"original-assertion\", \"assertionJson\":" + assertionJson +"}");
    }

    public void sendBreakpointHit(String breakpointIndex, String reportsAsJson, boolean isEvaluable) {
        log.info("pausing at " + breakpointIndex);
        getSession().getAsyncRemote().sendText(
                "{\"messageType\":\"breakpoint-hit\""
                        + ",\"testScriptIndex\":\"" + getTestScriptIndex() + "\""
                        + ",\"breakpointIndex\":\"" + breakpointIndex + "\""
                        + ",\"debugButtonLabel\":\"Resume\""
                        + ",\"isEvaluable\":\""+ isEvaluable +"\""
                        + ",\"testReport\":" + reportsAsJson  + "}"); // getModularEngine().reportsAsJson()

    }

    public static String getBreakpointIndex(String parentType, Integer parentIndex, Integer childPartIndex) {
        String breakpointIndex = String.format("%s%d", parentType, parentIndex);
        if (childPartIndex != null) {
            breakpointIndex += String.format(".%d", childPartIndex);
        }
        return breakpointIndex;
    }

    public boolean isBreakpoint(String breakpointIndex) {
        return getBreakpointSet().contains(breakpointIndex);
    }

    private boolean isWait() {
        boolean isWait = ! getKill().get();
        isWait = isWait && ! getResume().get();
        isWait = isWait && ! getEvaluateMode().get();

        return isWait;
    }


    public void pauseOnBreakpoint() {
//            log.info("About to lock and wait...");
        synchronized (getLock()) {
//                log.info("Locked!");
            while (isWait()) {
                try {
                    getLock().wait(); // Release the lock and wait for getResume to be True
                } catch (InterruptedException ie) {
                }
            }
            if (getResume().get()) {
                log.info("Resuming " +  getSession().getId());
            } else if (getKill().get()) {
                throw new Error("KILL session: " + getSession().getId());
            } else if (getEvaluateMode().get()) {
                log.info("Eval mode is true.");
            }
        }
    }
}
