package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.testEngine.engine.ModularReports;
import org.apache.log4j.Logger;

import java.io.IOException;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testReport"
// 4 - channelName   testSession__channelId
// 5 - testCollectionId
// 6 - testId
// return List of TestReports - main report first

public class GetTestReportRequest {
    private static Logger log = Logger.getLogger(GetTestReportRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testReport");
    }

    public GetTestReportRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetTestReportRequest");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        String testName = request.uriParts.get(6);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) {
            request.badRequest("Channel " + channelId + " does not exist");
            return;
        }
        String json = new ModularReports(request.ec, channelId, testCollection, testName).asJson();
        request.returnString(json);
        request.ok();
    }

}
