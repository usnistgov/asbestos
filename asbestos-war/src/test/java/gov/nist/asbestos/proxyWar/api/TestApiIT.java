package gov.nist.asbestos.proxyWar.api;

import gov.nist.asbestos.api.Channel;
import gov.nist.asbestos.api.TestResult;
import gov.nist.asbestos.api.impl.ApiConfig;
import gov.nist.asbestos.api.impl.ChannelRepo;
import gov.nist.asbestos.api.impl.TestRepo;
import gov.nist.asbestos.proxyWar.ITConfig;
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
