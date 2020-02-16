package gov.nist.asbestos.api;

import gov.nist.asbestos.sharedObjects.ChannelConfig;

import java.util.List;

public interface Test {
    TestLog run(Channel channel, TestParms testParms);
    List<TestLog> eval(Channel channel, int depth);
    boolean isClientTest();
}
