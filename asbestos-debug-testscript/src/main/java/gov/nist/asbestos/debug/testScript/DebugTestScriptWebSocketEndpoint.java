package gov.nist.asbestos.debug.testScript;


import gov.nist.asbestos.asbestosProxy.requests.Request;
import gov.nist.asbestos.sharedObjects.debug.DebugTestSessionId;
import gov.nist.asbestos.sharedObjects.debug.DebugWsSessionId;
import gov.nist.asbestos.sharedObjects.debug.TestScriptDebugState;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.debug.testScript.requests.DebugTestScriptRequest;
import org.apache.log4j.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;

@ServerEndpoint("/debugTestScript/{userType}")
public class DebugTestScriptWebSocketEndpoint {
    private static Logger log = Logger.getLogger(DebugTestScriptWebSocketEndpoint.class);
    /**
     * Key=The Websocket Session Id
     * Value=TestScriptDebugState
     */
    private static final ConcurrentHashMap<String, TestScriptDebugState> debugStateMap = new ConcurrentHashMap<>();
    /**
     * Key=The Websocket Session Id
     * Value=ExecutorService
     */
    private static final ConcurrentHashMap<String, ExecutorService> debugExecutorMap = new ConcurrentHashMap<>();
    /**
     * Key=DebugTestSessionId
     * Value=TestScript List
     */
    private static final ConcurrentHashMap<DebugTestSessionId, ConcurrentSkipListSet<DebugWsSessionId>> instanceMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(@PathParam("userType") String userType /* Required */,
                       Session session) throws IOException {
        String sessionId = session.getId();
        log.info(String.format("New session Id: %s. userType: %s.", sessionId, userType));
        log.info(String.format("Open WS session(s): %d. stateMap size: %d. executorMap size: %d.", session.getOpenSessions().size(), debugStateMap.size(), debugExecutorMap.size()));
        final Map<String, List<String>> myMap = session.getRequestParameterMap();
        String ftkTestSessionId /* ExclusivelyOptional */ = myMap.containsKey("ftkTestSessionId") ? myMap.get("ftkTestSessionId").get(0) : null;
        String testScriptIndex /* ExclusivelyOptional */ = myMap.containsKey("testScriptIndex") ? myMap.get("testScriptIndex").get(0) : null;
        String channelId /* ExclusivelyOptional */ = myMap.containsKey("channelId") ? myMap.get("channelId").get(0) : null;

        if ("developer".equals(userType)) {
            if (ftkTestSessionId != null && channelId != null && testScriptIndex != null) {
                DebugTestSessionId instanceId = new DebugTestSessionId(ftkTestSessionId, channelId);
                if (! instanceMap.containsKey(instanceId)) {
                    instanceMap.put(instanceId, new ConcurrentSkipListSet<>());
                }

                ConcurrentSkipListSet<DebugWsSessionId> mySet = instanceMap.get(instanceId);
                DebugWsSessionId wsSessionId = new DebugWsSessionId(sessionId, testScriptIndex);
                if (mySet.contains(wsSessionId)) {
                    // Only one debug request for the channel/testCollection/testScript is allowed
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "A debugger already exists for this TestScript: " + wsSessionId.toString()));
                } else {
                    mySet.add(wsSessionId);
                    debugStateMap.put(sessionId, new TestScriptDebugState(session, instanceId, testScriptIndex, new ConcurrentSkipListSet()));
                }
            }
        } else if ("admin".equals(userType)) {
            // TODO: Should be protected by a servlet filter
            // Admin does not use other parameters other than the userType
            // TODO: handle stop all debug threads request
            // map may not contain sessionId when session is automatically closed!
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        Objects.requireNonNull(message);

        log.info("raw onMessage: " + message);
        Map<String /* Request Type */, Object> myMap = new Gson().fromJson(message, Map.class);

        String cmd = (String) myMap.get("cmd");
        if (cmd == null || "".equals(cmd))
            return;

        if (cmd.equals("getExistingDebuggerList")) {
            sendExistingDebuggerList(session, myMap);
            return;
        } else if (cmd.equals("removeDebugger")) {
            DebugTestSessionId instanceId = new DebugTestSessionId(
                    (String) myMap.get("ftkTestSessionId"),
                    (String) myMap.get("channelId"));
            if (instanceMap.containsKey(instanceId)) {
                DebugWsSessionId wsSessionId = new DebugWsSessionId((String)myMap.get("testScriptIndex"));
                final ConcurrentSkipListSet<DebugWsSessionId> scriptIds = instanceMap.get(instanceId);

                Optional<DebugWsSessionId> realSessionId = scriptIds.stream()
                        .filter(s -> s.equals(wsSessionId))
                        .findFirst();
                if (realSessionId.isPresent()) {
                    TestScriptDebugState debugState = debugStateMap.get(realSessionId.get().getWsSessionId());
                    if (debugState != null) {
                        stopDebuggingTs(debugState);
                        debugState.setOnStop(s -> sendExistingDebuggerList(session, myMap)); // Return an updated debugger list
                        if (debugState.getSession().isOpen()) {
                            debugState.getSession().close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "Remove debug session requested by test session user"));
                        } else {
                            removeFromStateTracking(debugState);
                        }
                    } else {
                        log.error("removeDebugger error: debugState could not be found!");
                    }
                }

            }
            return;
        }

        TestScriptDebugState state = debugStateMap.get(session.getId());
        try {
            if (state == null) {
                log.error("debugState was not established.");
                return;
            }

            if (cmd.equals("beginDebug")) {
                String uriString = (String) myMap.get("uri");
                if (uriString != null) {
                    if (myMap.get("breakpointList") != null) {
                        List<String> myList = (List<String>) myMap.get("breakpointList");
//                        String testScriptIndex = (String) myMap.get("testScriptIndex");
                        state.getBreakpointSet().addAll(myList);
                        startDebuggerThread(state, uriString);
                    } else {
                        String exception = "breakpointList is empty.";
                        log.error(exception);
                        throw new RuntimeException(exception);
                    }
                }
            } else if (cmd.equals("resumeBreakpoint")) {
                List<String> updateList = (List<String>) myMap.get("breakpointList");
                doResumeBreakpoint(state, updateList);
            } else if (cmd.equals("stopDebug")) {
                stopDebuggingTs(state);
            } else if (cmd.equals("requestOriginalAssertion")) {
                String requestAnnotationsStr = (String)myMap.get("requestAnnotations");
                boolean isRequestAnnotations = Boolean.parseBoolean(requestAnnotationsStr);
                doRequestOriginalAssertion(state, isRequestAnnotations);
            } else if (cmd.equals("debugEvalAssertion")) {
                String base64String = (String)myMap.get("base64String");
                doDebugEvaluate(state, base64String);
            }

            // else if stepOver
            // else if Restart

        } catch (Exception ex) {
            if (state.isWait()) {
                stopDebuggingTs(state);
            } else {
               removeFromStateTracking(state);
            }
        }

    }

    private void sendExistingDebuggerList(Session session, Map<String, Object> myMap) {
        DebugTestSessionId instanceId = new DebugTestSessionId(
                (String)myMap.get("ftkTestSessionId"),
                (String)myMap.get("channelId"));
        if (instanceMap.containsKey(instanceId)) {
            final ConcurrentSkipListSet<DebugWsSessionId> scriptIds = instanceMap.get(instanceId);
            if (scriptIds != null && scriptIds.size() > 0) {
                List<String> myList =
                    scriptIds
                        .stream()
                        .map(DebugWsSessionId::getTestScriptIndex)
                        .map(TestScriptDebugState::quoteString)
                        .collect(Collectors.toList());
                String myString = String.join(",", myList);
                TestScriptDebugState.sendDebuggingTestScriptIndexes(session, myString);
                return;
            }
        }
        TestScriptDebugState.sendDebuggingTestScriptIndexes(session, "");
    }


    @OnClose
    public void onClose(Session session) {
        final String sessionId = session.getId();
        log.info("Close: " + sessionId + ". debugStateMap has: " + debugStateMap.keySet().toString());

        if (debugStateMap.containsKey(sessionId)) {
            ExecutorService service = debugExecutorMap.get(sessionId);
            if (service == null) {
                log.info(String.format("Service is null for for sessionId: %s", sessionId));
            }
            TestScriptDebugState debugState = debugStateMap.get(sessionId);
            if (debugState != null) {
                if (debugState.getStopDebug().get()) {
                    try {
                        service.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (Throwable t) {
                    } finally {
                      log.info(String.format("Session %s was terminated: %s", sessionId, service.isTerminated()));
                    }
                }
                if (service != null) {
                    log.info("is service terminated? " + service.isTerminated() + ". is shutdown? " + service.isShutdown());
                    if (service.isTerminated()) {
                        removeFromStateTracking(debugState);
                    }
                }
            }
        }
    }

    private void removeFromStateTracking(TestScriptDebugState debugState) {
        try {
            String sessionId = debugState.getSessionId();
            String testScriptIndex = debugState.getTestScriptIndex();
            DebugTestSessionId instanceId = debugState.getDebugTestSessionId();
            DebugWsSessionId wsSessionId = new DebugWsSessionId(sessionId, testScriptIndex);

            instanceMap.get(instanceId).remove(wsSessionId);
            if (debugState.getOnStop() != null) {
                debugState.getOnStop().accept(Optional.empty());
            }
            debugExecutorMap.remove(sessionId);
            debugStateMap.remove(sessionId);
        } catch (Exception ex) {
            log.error("removeFromStateTracking: " + ex.toString());
        }
    }

    @OnError
    public void onError(Throwable t) {
       log.error(t.toString());
    }

    private void startDebuggerThread(TestScriptDebugState state, String uriString) throws Exception {
        String sessionId = state.getSessionId();
        Request request = new Request(uriString, Installation.instance().externalCache());

        if (DebugTestScriptRequest.isRequest(request)) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            if (executorService != null) {
                debugExecutorMap.put(sessionId, executorService);
                DebugTestScriptRequest debugger = new DebugTestScriptRequest(request, state);

                executorService.execute(debugger);
                executorService.shutdown();
            }
        }
    }

    private void doResumeBreakpoint(TestScriptDebugState state, List<String> updateList) {
        if (updateList != null) {
            state.getBreakpointSet().clear();
            state.getBreakpointSet().addAll(updateList);
        }

        synchronized (state.getLock()) {
           state.getResume().set(true);
           state.getLock().notify();
        }
    }

    private void doRequestOriginalAssertion(TestScriptDebugState state, boolean requestAnnotations) {
        synchronized (state.getLock()) {
            state.resetEvalJsonString();
            state.getRequestAnnotations().set(requestAnnotations);
            state.getDebugEvaluateModeWasRequested().set(true);
            state.getLock().notify();
        }
    }

    private void doDebugEvaluate(TestScriptDebugState state, String base64String) {
        if (base64String != null) {
            state.setEvalJsonString(new String(Base64.getDecoder().decode(base64String)));
            synchronized (state.getLock()) {
                state.getDebugEvaluateModeWasRequested().set(true);
                state.getLock().notify();
            }
        }
    }


    /**
     * Causes an exception thrown inside the TestEngine which stop test execution where it was paused at.
     * @param state
     */
    private static void stopDebuggingTs(TestScriptDebugState state) {
        String sessionId = state.getSessionId();
        log.info("stopDebuggingTs Session: " + sessionId);
        synchronized (state.getLock()) {
            state.cancelResumeMode();
            state.resetEvalModeWasRequested();
            state.getStopDebug().set(true);
            state.getLock().notify();
        }
    }


}
