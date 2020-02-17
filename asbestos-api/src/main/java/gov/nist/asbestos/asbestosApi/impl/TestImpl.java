package gov.nist.asbestos.asbestosApi.impl;

import gov.nist.asbestos.asbestosApi.*;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.List;

public class TestImpl implements Test {
    // based on test definition
    private boolean client;
    private TestScript testScript;
    // from last run
    private Channel channel;
    private TestParms testParms;
    private List<TestReport> testReports;  // only one if server test

    @Override
    public TestLog run(Channel channel, TestParms testParms) {
        return null;
    }

    @Override
    public List<TestLog> eval(Channel channel, int depth) {
        return null;
    }

    @Override
    public boolean isClientTest() {
        return client;
    }

}
