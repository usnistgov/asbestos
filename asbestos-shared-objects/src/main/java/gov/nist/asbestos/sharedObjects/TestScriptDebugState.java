package gov.nist.asbestos.sharedObjects;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.Session;

public class TestScriptDebugState {
    private Object lock;
    private AtomicBoolean resume;
    private AtomicBoolean kill;
    private AtomicBoolean evaluateMode;
    private String testScriptIndex; /* TestCollectionIndex + TestScriptIndex */
    private ConcurrentSkipListSet breakpointSet;
    private Session session;
    private String userType;
    private String evalJsonString;
    private String currentExecutionIndex;
    private List<String> parentExecutionIndex = new ArrayList<>();
    TestScriptDebugInterface debugInterface = null;

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

    Session getSession() {
        return session;
    }

    public String getSessionId() {
        return getSession().getId();
    }

    public AtomicBoolean getDebugEvaluateMode() {
        return evaluateMode;
    }

    public String getEvalJsonString() {
        return evalJsonString;
    }

    public void setEvalJsonString(String evalJsonString) {
        this.evalJsonString = evalJsonString;
    }

    /**
     * Clear the variable state. An empty eval Json is an indicator for requesting original-assertion if no assertion data is provided
     */
    public void resetEvalJsonString() {
        this.evalJsonString = null;
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
    public void resetEvalMode() {
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

    public void sendDebugAssertionEvalResultStr(String str, String markdownMessage) {
        String base64 = (markdownMessage != null && markdownMessage.length() > 0) ? Base64.getEncoder().encodeToString(markdownMessage.getBytes()) : "";
        getSession().getAsyncRemote().sendText("{\"messageType\":\"eval-assertion-result\", \"resultMessage\":\"" + str +"\","
                + "\"markdownMessage\":\"" + base64 + "\"}");
    }

    public void sendBreakpointHit(boolean isEvaluable) {
        if (! hasInterface())
            return;
        String reportsAsJson = debugInterface.getLogAtBreakpoint();
        String breakpointIndex = getCurrentExecutionIndex();
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

    public boolean isBreakpoint() {
        return isBreakpoint(getCurrentExecutionIndex());
    }

    public boolean isBreakpoint(String breakpointIndex) {
        return getBreakpointSet().contains(breakpointIndex);
    }

    private boolean isWait() {
        boolean isWait = ! getKill().get();
        isWait = isWait && ! getResume().get();
        isWait = isWait && ! getDebugEvaluateMode().get();

        return isWait;
    }


    public void waitOnBreakpoint() {
        cancelResumeMode();

        synchronized (getLock()) {
            while (isWait()) { // Condition must be false to exit the wait and to protect from spurious wake-ups
                try {
                    getLock().wait(); // Release the lock and wait for getResume to be True
                } catch (InterruptedException ie) {
                }
            }
            if (getResume().get()) {
                log.info("Resuming " +  getSession().getId());
            } else if (getKill().get()) {
                throw new Error("KILL session: " + getSession().getId()); // This needs to throw a custom exception that does not show up in the test report
            } else if (getDebugEvaluateMode().get()) {
                log.info("Eval mode is true.");
            }
        }
    }

    public String getCurrentExecutionIndex() {
        String parentIndex = String.join("." , parentExecutionIndex);
        if (parentIndex != "") {
            return parentIndex + "." + currentExecutionIndex;
        }
        else
            return currentExecutionIndex;
    }

    public String setCurrentExecutionIndex(String parentType, Integer parentIndex, Integer childPartIndex) {
            String index = getBreakpointIndex(parentType, parentIndex, childPartIndex);
            currentExecutionIndex = index;
            return currentExecutionIndex;
    }

    public boolean pushParentExecutionIndex() {
        return parentExecutionIndex.add(currentExecutionIndex);
    }

    public String popParentExecutionIndex() {
       return parentExecutionIndex.remove(parentExecutionIndex.size()-1);
    }

    public boolean hasInterface() {
        return debugInterface != null;
    }

    public void pauseIfBreakpoint() {
        boolean isBreakpoint = isBreakpoint();
        if (isBreakpoint) {
            if (hasInterface()) {
                debugInterface.onBreakpoint();
                sendBreakpointHit(false);
                waitOnBreakpoint();
            }
        }
    }

    public TestScriptDebugInterface getDebugInterface() {
        return debugInterface;
    }

    public void setDebugInterface(TestScriptDebugInterface debugInterface) {
        this.debugInterface = debugInterface;
    }
}
