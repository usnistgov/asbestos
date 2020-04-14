package gov.nist.asbestos.testScriptDebugger;


import gov.nist.asbestos.asbestosProxy.requests.Request;
import gov.nist.asbestos.sharedObjects.TestScriptDebugState;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.log4j.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;


@ServerEndpoint("/testScriptDebugger")
public class TestScriptDebuggerWebSocketServer {
    private static Logger log = Logger.getLogger(TestScriptDebuggerWebSocketServer.class);
    private static final ConcurrentHashMap<String, TestScriptDebugState> debugStateMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ExecutorService> debugExecutorMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("onOpen: " + session.getId());
        log.info(String.format("There are %d open sessions.", session.getOpenSessions().size()));
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        log.info("raw onMessage: " + message);

        if (message != null) {
            Map<String, Object> myMap = new Gson().fromJson(message, Map.class);

            if (myMap.get("uri") != null) {
                String uriString = (String) myMap.get("uri");
                if (uriString != null) {
                    if (myMap.get("breakpointList") != null) {
                        List<String> myList = (List<String>) myMap.get("breakpointList");
                        String testScriptIndex = (String) myMap.get("testScriptIndex");
                        startDebuggerThread(session, uriString, testScriptIndex, myList);
                    } else {
                        String exception = "breakpointList is empty.";
                        log.error(exception);
                        throw new RuntimeException(exception);
                    }
                }
            } else if (myMap.get("resumeBreakpoint") != null) {
               doResumeBreakpoint(session);
            } else if (myMap.get("killDebug") != null) {
                killSession(session);
            }

        }

    }

    @OnClose
    public void onClose(Session session) {
        final String sessionId = session.getId();
        log.info("Close: " + sessionId);
        // Close off threads created by session
        if (debugExecutorMap != null && debugStateMap.get(sessionId).getKill().get()) {
            ExecutorService service = debugExecutorMap.get(sessionId);
            try {
//                if (debugStateMap.get(sessionId).getResume().get()) {
////                    debugStateMap.get(sessionId).getBreakpointSet().clear();
//                    killSession(session);
//                }
                service.awaitTermination(2, TimeUnit.SECONDS);
            } catch (Throwable t) {
            } finally {
                log.info(String.format("Session %s was terminated: %s", sessionId, service.isTerminated()));
            }
        }

    }

    @OnError
    public void onError(Throwable t) {
       log.error(t.toString());
    }

    private void startDebuggerThread(Session session, String uriString, String testScriptIndex, List<String> breakpointList) throws Exception {
        String sessionId = session.getId();
        Request request = new Request(uriString, Installation.instance().externalCache());

        if (TestScriptDebuggerThread.isRequest(request)) {
            // Only one debug request for the channel/testCollection is allowed
            if (! debugExecutorMap.contains(sessionId)) {

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                debugExecutorMap.put(sessionId, executorService);

                TestScriptDebuggerThread debugger = new TestScriptDebuggerThread(request, session, testScriptIndex);
                debugStateMap.put(sessionId, debugger.getState());
                debugger.getState().getBreakpointSet().addAll(breakpointList);

                executorService.submit(debugger);
                executorService.shutdown();
            }
        }
    }

    private void doResumeBreakpoint(Session session) {
        String sessionId = session.getId();
        TestScriptDebugState state = debugStateMap.get(sessionId);
        synchronized (state.getLock()) {
           state.getResume().set(true);
           state.getLock().notify();
        }
    }

    private void killSession(Session session) {
        String sessionId = session.getId();
        log.info("killSession: " + sessionId);
        debugStateMap.get(session.getId()).getKill().set(true);
        TestScriptDebugState state = debugStateMap.get(sessionId);
        synchronized (state.getLock()) {
            state.getKill().set(true);
            state.getLock().notify();
        }
    }


}
