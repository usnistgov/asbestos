package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testReport"
// 4 - channelName   testSession__channelId
// 5 - testCollectionId
// 6 - testId
// return a test log

public class GetTestReportRequest {
    private static Logger log = Logger.getLogger(GetTestReportRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testReport");
    }

    public GetTestReportRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetTestReport");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        String testName = request.uriParts.get(6);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) throw new Error("Channel does not exist");

        File testLog = request.ec.getTestLog(channelId, testCollection, testName);
        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(testLog.toString())));
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
        Returns.returnString(request.resp, json);
        log.info("OK");
    }
}
