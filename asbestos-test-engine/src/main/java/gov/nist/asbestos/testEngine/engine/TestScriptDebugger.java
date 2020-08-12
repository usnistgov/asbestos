package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.model.api.annotation.Description;
import com.google.gson.Gson;
import gov.nist.asbestos.sharedObjects.debug.StopDebugTestScriptException;
import gov.nist.asbestos.sharedObjects.debug.TestScriptDebugInterface;
import gov.nist.asbestos.sharedObjects.debug.TestScriptDebugState;
import gov.nist.asbestos.sharedObjects.debug.TsEnumerationCodeExtractor;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.nist.asbestos.sharedObjects.debug.TestScriptDebugState.quoteString;

public class TestScriptDebugger implements TestScriptDebugInterface {
    private static Logger log = Logger.getLogger(TestScriptDebugger.class);
    /**
     * An implementation of the TestScript Test Engine
     */
    TestEngine te;
    /**
     * If testScriptDebugState is null, then TestScript is being run normally. ie., TestScript is not being debugged.
     */
    private TestScriptDebugState state;

    public TestScriptDebugger(TestEngine testEngine, TestScriptDebugState state) {
        this.te = testEngine;
        this.state = state;
    }

    public ModularEngine getMyModularEngine() {
        ModularEngine me = te.getModularEngine();
        if (me == null) {
            if (te.parent != null && te.parent.getModularEngine() !=null) {
                me = te.parent.getModularEngine();
            }
        }
        return me;
    }
    @Override
    public void onBreakpoint() {
        ModularEngine me = this.getMyModularEngine();
        if (me != null) {
            me.saveLogs(); // Without this getTestReportsAsJson is empty
        } else {
            log.error("getModularEngine is null: log cannot be saved!");
        }
    }

    @Override
    public String getLogAtBreakpoint() {
        ModularEngine me = this.getMyModularEngine();
        if (me != null) {
            return me.reportsAsJson();
        } else {
            log.error("getModularEngine is null!");
            getState().sendUnexpectedError();
            return "";
        }
    }

    @Override
    public TestScriptDebugState getState() {
        return state;
    }

    @Override
    public void pauseIfBreakpoint() {
        boolean isBreakpoint = state.isBreakpoint();
        if (isBreakpoint) {
            onBreakpoint();
            sendBreakpointHit(false);
            waitOnBreakpoint();
        }
    }


    @Override
    public void pauseIfBreakpoint(final String parentType, final Integer parentIndex, final TestScript.SetupActionAssertComponent assertComponent, final Integer childPartIndex) {
        TestScriptDebugState state = getState();
        state.setCurrentExecutionIndex(parentType, parentIndex, childPartIndex);

        boolean isBreakpoint = state.isBreakpoint();
        if (isBreakpoint) {
            onBreakpoint();
            sendBreakpointHit(true);
            final List<String> evalElementList = Arrays.asList("label","description","direction","compareToSourceId","compareToSourceExpression","compareToSourcePath","contentType","expression","headerField","minimumId",
                    "navigationLinks","operator","path","requestMethod","requestURL","resource","response","responseCode","sourceId","validateProfileId","value","warningOnly");
            do {
                // Must Pause first before Eval can be requested
                waitOnBreakpoint(); // if eval, exit pause
                if (state.getDebugEvaluateModeWasRequested().get()) { // Only assertion-eval is supported for now. Need to address TestScript Operations later
                    state.resetEvalModeWasRequested();
                    String evalJsonString = state.getEvalJsonString();
                    if (evalJsonString == null) {
                        // Prepare user-selectable type information
                        // "valueTypes" : {"direction" : [{"codeValue":"req","displayName":"","definition":""},...],
                        try {
                            Map<String,String> enumeratedTypeValues = new HashMap<>();
                            Field f = TestScript.SetupActionAssertComponent.class.getDeclaredField("direction");
                            if (f != null) {
                                String formalDefinition = f.getAnnotation(Description.class).formalDefinition();
                                if (f.getType().isAssignableFrom(Enumeration.class)) {
                                    Enumeration fhirEnum = ((Enumeration)f.get(TestScript.SetupActionAssertComponent.class));
                                    Object e = fhirEnum.getValue(); // enum class implicitly extends java.lang.Enum
                                    if (e != null) {
                                        if (e.getClass().getEnumConstants() != null) {
                                            List<String> quotedValueTypes =
                                                    Arrays.stream((Enum[])e.getClass().getEnumConstants())
                                                            .filter(e1 -> ! e1.name().equals("NULL"))
                                                            .map(new TsEnumerationCodeExtractor())
                                                            .collect(Collectors.toList());
                                            if (! quotedValueTypes.isEmpty()) {
                                                enumeratedTypeValues.put("direction", quotedValueTypes.get(0));
                                            }

                                        }

                                    }
                                }
                            }

                        } catch (Exception ex) {
                            // formalDefinition = "Not available.";
                        }
                        List<String> quotedValueTypes =
                                Arrays.stream(TestScript.AssertionDirectionType.values())
                                        .filter(s -> ! s.equals(TestScript.AssertionDirectionType.NULL))
                                        .map(s -> TestScriptDebugState.formatAsSelectOptionData(s.getDisplay(), s.toCode(), s.getDefinition()))
                                        .collect(Collectors.toList());
                        // Property names must exist in evalElementList
                        String valueTypes = String.format("{\"direction\": {"
                                + quoteString("formalDefinition") + ":"+ quoteString("formal def. place holder")
                                +", \"values\":[%s] } }", String.join(",", quotedValueTypes));
                        // If evalJsonString is empty, Send original assertion as a template for the user to edit an assertion
                        String assertionJsonStr = new Gson().toJson(assertComponent);
                        state.sendAssertionStr(assertionJsonStr, valueTypes);
                    } else {
                        // Eval
                        ListIterator<String> it = evalElementList.listIterator();
                        TestScript.SetupActionAssertComponent copy = null;
                        boolean copyException = false;
                        try {
                            Map<String, String> myMap = new Gson().fromJson(evalJsonString, Map.class);
                            if (! myMap.keySet().containsAll(evalElementList)) {
                                throw new RuntimeException("myMap does not contain all the required keys");
                            }
                            copy = assertComponent.copy();
                            copy.setLabel(myMap.get(it.next()));
                            copy.setDescription(myMap.get(it.next()));
                            copy.setDirection(TestScript.AssertionDirectionType.fromCode(myMap.get(it.next())));
                            copy.setCompareToSourceId(myMap.get(it.next()));
                            copy.setCompareToSourceExpression(myMap.get(it.next()));
                            copy.setCompareToSourcePath(myMap.get(it.next()));
                            copy.setContentType(myMap.get(it.next()));
                            copy.setExpression(myMap.get(it.next()));
                            copy.setHeaderField(myMap.get(it.next()));
                            copy.setMinimumId(myMap.get(it.next()));
                            copy.setNavigationLinks(Boolean.parseBoolean(myMap.get(it.next())));
                            copy.setOperator(TestScript.AssertionOperatorType.fromCode(myMap.get(it.next())));
                            copy.setPath(myMap.get(it.next()));
                            copy.setRequestMethod(TestScript.TestScriptRequestMethodCode.fromCode(myMap.get(it.next())));
                            copy.setRequestURL(myMap.get(it.next()));
                            copy.setResource(myMap.get(it.next()));
                            copy.setResponse(TestScript.AssertionResponseTypes.fromCode(myMap.get(it.next())));
                            copy.setResponseCode(myMap.get(it.next()));
                            copy.setSourceId(myMap.get(it.next()));
                            copy.setValidateProfileId(myMap.get(it.next()));
                            copy.setValue(myMap.get(it.next()));
                            copy.setWarningOnly(Boolean.parseBoolean(myMap.get(it.next())));
                        } catch (Exception ex) {
                            copyException = true;
                            state.sendDebugAssertionEvalResultStr("Exception: ", ex.getMessage(), (it.hasPrevious()?it.previous():""));
                        }
                        if (copy != null && ! copyException) {
                            String typePrefix = "contained.action";
                            TestReport.SetupActionAssertComponent actionReport = new TestReport.SetupActionAssertComponent();
                            te.doAssert(typePrefix, copy, actionReport);
                            String code = actionReport.getResult().toCode();
                            if ("fail".equals(code)) {
                                log.info("copy eval failed.");
                            } else if ("error".equals(code)) {
                                log.info("copy eval error.");
                            }
                            state.sendDebugAssertionEvalResultStr(code, actionReport.getMessage(), "");
                        }
                    }
                }
            } while (! state.getResume().get() && ! state.getStopDebug().get());
        }
    }

    @Override
    public void pauseIfBreakpoint(String parentType, Integer parentIndex) {
        pauseIfBreakpoint(parentType, parentIndex, null, false);
    }

    @Override
    public void pauseIfBreakpoint(String parentType, Integer parentIndex, Integer childPartIndex, boolean hasImportExtension) {
        TestScriptDebugState state = getState();
        state.setHasImportExtension(hasImportExtension);
        state.setCurrentExecutionIndex(parentType, parentIndex, childPartIndex);
        pauseIfBreakpoint();
    }


    @Override
    public void waitOnBreakpoint() {
        TestScriptDebugState state = getState();
        state.cancelResumeMode();

        synchronized (state.getLock()) {
            while (state.isWait()) { // Condition must be false to exit the wait and to protect from spurious wake-ups
                try {
                    state.getLock().wait(); // Release the lock and wait for getResume to be True
                } catch (InterruptedException ie) {
                }
            }
            if (state.getResume().get()) {
                log.info("Resuming " +  state.getSession().getId());
            } else if (state.getStopDebug().get()) {
//                throw new Error("KILL session: " + getSession().getId()); // This needs to throw a custom exception that does not show up in the test report
                throw new StopDebugTestScriptException("STOP debug session: " + state.getSession().getId());
            } else if (state.getDebugEvaluateModeWasRequested().get()) {
                log.info("Eval mode is true.");
            }
        }
    }

    public void sendBreakpointHit(boolean isEvaluable) {
        TestScriptDebugState state = getState();
        String reportsAsJson = getLogAtBreakpoint();
        String breakpointIndex = state.getCurrentExecutionIndex();
        log.info("pausing at " + breakpointIndex);
        state.getSession().getAsyncRemote().sendText(
                "{\"messageType\":\"breakpoint-hit\""
                        + ",\"testScriptIndex\":\"" + state.getTestScriptIndex() + "\""
                        + ",\"breakpointIndex\":\"" + breakpointIndex + "\""
                        + ",\"debugButtonLabel\":\"Resume\""
                        + ",\"isEvaluable\":\""+ isEvaluable +"\""
                        + ",\"testReport\":" + reportsAsJson  + "}"); // getModularEngine().reportsAsJson()

    }


}
