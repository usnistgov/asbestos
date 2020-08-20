package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.model.api.annotation.Description;
import com.google.gson.Gson;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.sharedObjects.debug.AssertionFieldDescription;
import gov.nist.asbestos.sharedObjects.debug.AssertionFieldSupport;
import gov.nist.asbestos.sharedObjects.debug.AssertionFieldValueDescription;
import gov.nist.asbestos.sharedObjects.debug.StopDebugTestScriptException;
import gov.nist.asbestos.sharedObjects.debug.TestScriptDebugInterface;
import gov.nist.asbestos.sharedObjects.debug.TestScriptDebugState;
import gov.nist.asbestos.sharedObjects.debug.TsEnumerationCodeExtractor;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                        // Original assertion was requested
                        // Prepare user-selectable type information
                        // "valueTypes" : {"direction" : [{"codeValue":"req","displayName":"","definition":""},...],
                        List<AssertionFieldDescription> fieldDescriptions = new ArrayList<>();
                        List<AssertionFieldDescription> overrideFields = new ArrayList<>();
                        AssertionFieldSupport fieldSupport = new AssertionFieldSupport();
                        if (state.getRequestAnnotations().get()) {
                            // gather static FHIR enumeration values and other field support values as requested only the first time around
                            setAssertionEnumerationTypes(evalElementList, fieldDescriptions);
                            fieldSupport.setFhirEnumerationTypes(fieldDescriptions);
                            List<String> resourceNames = new ArrayList<String>(Ref.getResourceNames());
                            Collections.sort(resourceNames);
                            overrideFields.add(formatField("resource", new ArrayList<String>(resourceNames)));
                            List<String> contentTypeList = new ArrayList<String>(Format.getFormats());
                            Collections.sort(contentTypeList);
                            overrideFields.add(formatField("contentType", contentTypeList));
                            fieldSupport.setOverrideFieldTypes(overrideFields);
                        }
                        Set<String> fixtureIds = te.getFixtureMgr().keySet();
                        AssertionFieldDescription fixtureIdsDesc = formatField("sourceId", new ArrayList<String>(fixtureIds));

                        // When the evalJsonString is empty, Send original assertion as a template for the user to edit an assertion
                        String assertionJsonStr = new Gson().toJson(assertComponent);
                        String fieldSupportStr = new Gson().toJson(fieldSupport);
                        String fixtureIdsStr = new Gson().toJson(fixtureIdsDesc);

                        state.sendAssertionStr(assertionJsonStr, fieldSupportStr, fixtureIdsStr);
                    } else {
                        // Eval was requested
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

    private AssertionFieldDescription formatField(String fieldName, List<String> values) {
        List<AssertionFieldValueDescription> fieldValues = new ArrayList<>();
        for (String s : values) {
            fieldValues.add(new AssertionFieldValueDescription(s, s, ""));
        }
       AssertionFieldDescription fieldDescription =  new AssertionFieldDescription(fieldName, "", "", fieldValues);
        return fieldDescription;
    }

    private void setAssertionEnumerationTypes(List<String> evalElementList, List<AssertionFieldDescription> fieldDescriptions) {
        try {
            for (String eStr : evalElementList) {
                String shortDefinition = "";
                String formalDefinition = "";
                List<AssertionFieldValueDescription> fieldValueTypes = new ArrayList<>();
                // Property names must exist in evalElementList
                Field f = TestScript.SetupActionAssertComponent.class.getDeclaredField(eStr);
                if (f != null) {
                    Description annotation = f.getAnnotation(Description.class);
                    if (annotation != null) {
                        shortDefinition = annotation.shortDefinition();
                        formalDefinition = annotation.formalDefinition();
                    }
                    if (f.getType().isAssignableFrom(Enumeration.class)) {
                        Type type = f.getGenericType();
                        if (type instanceof ParameterizedType) {
                            ParameterizedType paramType = (ParameterizedType)type;
                            if (paramType.getActualTypeArguments().length == 1) {
                                String typeArg = paramType.getActualTypeArguments()[0].getTypeName();
                                if (typeArg != null && ! "".equals(typeArg)) {
                                    Optional<Class<?>> optionalClass = Arrays.asList(TestScript.class.getDeclaredClasses()).stream()
                                            .filter(s -> s.getName().equals(typeArg))
                                            .findFirst();
                                    if (optionalClass.isPresent()) {
                                        for (Object o : optionalClass.get().getEnumConstants()) {
                                            if (o instanceof Enum) {
                                                if (! ((Enum) o).name().equals("NULL")) {
                                                    fieldValueTypes.add(new TsEnumerationCodeExtractor().apply(o));
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }

                    }
                }
                fieldDescriptions.add(new AssertionFieldDescription(eStr, shortDefinition, formalDefinition, fieldValueTypes));
            }

        } catch (Exception ex) {
            // formalDefinition = "Not available.";
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
