package gov.nist.asbestos.sharedObjects.debug;

public interface TestEngineDebugInterface {
    void onBreakpoint();
    String getLogAtBreakpoint();
}
