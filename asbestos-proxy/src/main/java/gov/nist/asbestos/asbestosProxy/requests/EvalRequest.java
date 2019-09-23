package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import org.apache.log4j.Logger;
// 0 - empty
// 1 - app context
// 2 - "eval"
// 3   testSession__channelId
// 4 - testCollectionId
// 5 - testId
// Prepare the channel for an evaluation (client test)
// payload is ignored

public class EvalRequest {
    private static Logger log = Logger.getLogger(EvalRequest.class);
    private Request request;

    public static String NEXT_CLIENT_TEST = "nextClientTest";

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 6 && request.uriParts.get(2).equals("eval");
    }

    public EvalRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("EvalRequest");
        String channelId = request.uriParts.get(3);
        ChannelConnector.connect(request.resp, request.externalCache, channelId); // no exception => channel exists
        String testCollectionId = request.uriParts.get(4);
        String testId = request.uriParts.get(5);
        if (testCollectionId.isEmpty() || testId.isEmpty()) {
            request.resp.setStatus(request.resp.SC_BAD_REQUEST);
            return;
        }
        request.getSession().setAttribute(NEXT_CLIENT_TEST, testCollectionId + "/" + testId);
        request.resp.setStatus(request.resp.SC_OK);
    }
}
