package gov.nist.asbestos.asbestosApi;

import java.util.List;

public interface Test {
    TestLog run(Channel channel, TestParms testParms);
    List<TestLog> eval(Channel channel, int depth);
    boolean isClientTest();
}
