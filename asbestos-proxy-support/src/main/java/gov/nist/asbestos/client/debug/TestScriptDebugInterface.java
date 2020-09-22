package gov.nist.asbestos.client.debug;

public interface TestScriptDebugInterface {
    void onBreakpoint();
    String getLogAtBreakpoint();
}
