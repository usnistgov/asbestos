package gov.nist.asbestos.testScriptDebugger;


import gov.nist.asbestos.asbestosProxy.requests.Request;
import gov.nist.asbestos.sharedObjects.TestScriptDebugState;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.log4j.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.gson.Gson;


@ServerEndpoint("/testScriptDebugger")
public class TestScriptDebuggerWebSocketServer {
    private static Logger log = Logger.getLogger(TestScriptDebuggerWebSocketServer.class);
    private static final ConcurrentHashMap<String, TestScriptDebugState> testScriptDebugStateMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ExecutorService> testScriptDebugExecutorMap = new ConcurrentHashMap<>();

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
                        doUri(session, uriString, myList);
                    } else {
                        String exception = "breakpointList is empty.";
                        log.error(exception);
                        throw new RuntimeException(exception);
                    }
                }
            } else if (myMap.get("resumeBreakpoint") != null) {
               doResumeBreakpoint(session);
            }

        }

    }

    @OnClose
    public void onClose(Session session) {
        log.info("Close: " + session.getId());
    }

    /*
    @OnError
    public void onError(Throwable t) {
       log.error(t.toString());
    }
    *
     */

    private void doUri(Session session, String uriString, List<String> breakpointList) throws Exception {
        String sessionId = session.getId();
        Request request = new Request(uriString, Installation.instance().externalCache());

        if (TestScriptDebugger.isRequest(request)) {
            // Only one debug request for the channel/testCollection is allowed
            if (! testScriptDebugExecutorMap.contains(sessionId)) {

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                testScriptDebugExecutorMap.put(sessionId, executorService);

                TestScriptDebugger debugger = new TestScriptDebugger(request, session);
                testScriptDebugStateMap.put(sessionId, debugger.getState());
                debugger.getState().getBreakpointSet().addAll(breakpointList);

                Future<String> future = executorService.submit(debugger);
                executorService.shutdown();

                String finalReport = future.get();
                if (finalReport != null) {
                    session.getAsyncRemote().sendText("{\"messageType\":\"final-report\", \"testReport\":" + finalReport + "}");
                }
            }
        }
    }

    private void doResumeBreakpoint(Session session) {
        String sessionId = session.getId();
        TestScriptDebugState state = testScriptDebugStateMap.get(sessionId);
        synchronized (state.getLock()) {
           state.getResume().set(true);
           state.getLock().notify();
        }
    }

}
