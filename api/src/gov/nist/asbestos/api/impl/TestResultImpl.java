package gov.nist.asbestos.api.impl;

import gov.nist.asbestos.api.Channel;
import gov.nist.asbestos.api.TestResult;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

public class TestResultImpl implements TestResult {
    private TestScript testScript;
    private TestReport testReport;
    private Channel channel;
    private String testCollection;
    private String testName;

    @Override
    public TestScript getTestScript() {
        return testScript;
    }

    @Override
    public TestReport getTestReport() {
        return testReport;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getTestCollection() {
        return testCollection;
    }

    @Override
    public String getTestName() {
        return testName;
    }

    TestResultImpl setTestScript(TestScript testScript) {
        this.testScript = testScript;
        return this;
    }

    TestResultImpl setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }

    TestResultImpl setChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    TestResultImpl setTestCollection(String testCollection) {
        this.testCollection = testCollection;
        return this;
    }

    TestResultImpl setTestName(String testName) {
        this.testName = testName;
        return this;
    }
}
