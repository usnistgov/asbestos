package gov.nist.asbestos.proxyWar.api;

import gov.nist.asbestos.asbestosApi.Channel;
import gov.nist.asbestos.asbestosApi.TestResult;
import gov.nist.asbestos.asbestosApi.impl.ApiConfig;
import gov.nist.asbestos.asbestosApi.impl.ChannelRepo;
import gov.nist.asbestos.asbestosApi.impl.TestRepo;
import gov.nist.asbestos.proxyWar.support.ITConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestApiIT {

    @Test
    void runTest() throws Exception {
        ApiConfig.setAsbestosBase(ITConfig.getAsbestosBase());

        Channel defaultChannel = ChannelRepo.get("default", "default");
        assertNotNull(defaultChannel);

        TestResult testResult = TestRepo.runServerTest(defaultChannel,
                "MHD_DocumentRecipient_minimal",
                "MinimalSubmission");
        assertNotNull(testResult);
        assertNotNull(testResult.getTestScript());
        assertNotNull(testResult.getTestReport());
        assertNotNull(testResult.getChannel());
        assertNotNull(testResult.getTestCollection());
        assertNotNull(testResult.getTestName());
    }
}
