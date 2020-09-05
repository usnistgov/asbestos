package gov.nist.asbestos.testEngine.engine;

public interface TestDef {

    String getTestId();
    String getTestCollectionId();
    String getTestSessionId();
    String getChannelId();  // simple name
}
