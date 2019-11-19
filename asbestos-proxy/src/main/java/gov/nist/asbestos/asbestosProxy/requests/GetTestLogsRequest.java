package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testlog"
// 4 - channelName   testSession__channelId
// 5 - testCollectionId
// return list of test logs (TestReport resources)

public class GetTestLogsRequest {
    private static Logger log = Logger.getLogger(GetTestLogsRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 6 && request.uriParts.get(3).equals("testlog");
    }

    public GetTestLogsRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetTestLogs");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) throw new Error("Channel does not exist");

        StringBuilder buf = new StringBuilder();
        buf.append("{\n");
        List<File> testLogs = request.ec.getTestLogs(channelId, testCollection);
        boolean first = true;
        for (File testLog : testLogs) {
            String name = testLog.getName();
            name = name.split("\\.")[0];
            String json;
            try {
                if (!first)
                    buf.append(",\n");
                json = new String(Files.readAllBytes(Paths.get(testLog.toString())));
                buf.append("\"").append(name).append("\": ").append(json);
                first = false;
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }
        }
        buf.append("\n}");
        String theString = buf.toString();
        Returns.returnString(request.resp, theString);
        log.info("OK");
    }
}
