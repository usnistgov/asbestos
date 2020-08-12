package gov.nist.asbestos.sharedObjects.debug;

import org.hl7.fhir.r4.model.TestScript;

public interface TestScriptDebugInterface {
    void onBreakpoint();
    String getLogAtBreakpoint();
    TestScriptDebugState getState();
    void pauseIfBreakpoint();
    void pauseIfBreakpoint(String parentType, Integer parentIndex);
    void pauseIfBreakpoint(String parentType, Integer parentIndex, Integer childPartIndex, boolean hasImportExtension);
    void pauseIfBreakpoint(final String parentType, final Integer parentIndex, final TestScript.SetupActionAssertComponent assertComponent, final Integer childPartIndex);
    void waitOnBreakpoint();


    }
