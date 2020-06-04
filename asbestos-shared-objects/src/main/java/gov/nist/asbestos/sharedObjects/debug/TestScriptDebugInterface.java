package gov.nist.asbestos.sharedObjects.debug;

public interface TestScriptDebugInterface {
    void onBreakpoint();
    String getLogAtBreakpoint();
}
