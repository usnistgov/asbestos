package gov.nist.asbestos.asbestosApi;

import gov.nist.asbestos.sharedObjects.ChannelConfig;

import java.util.List;

public interface TestCollection {
    List<Test> getTests();
    List<String> getTestNames();
    List<TestLog> run(TestSession testSession, ChannelConfig channel, TestParms testParms);
    boolean isClientTest();
}
