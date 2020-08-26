package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.Request;
import org.apache.log4j.Logger;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "eval"
// 4   testSession__channelId
// 5 - testCollectionId
// 6 - testId
// Prepare the channel for an evaluation (client test)
// payload is ignored

public class EvalRequest {
    private static Logger log = Logger.getLogger(EvalRequest.class);
    private Request request;

    public static String NEXT_CLIENT_TEST = "nextClientTest";

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("eval");
    }

    public EvalRequest(Request request) {
        this.request = request;
    }

    public void run() {
        request.announce("EvalRequest");
        String channelId = request.uriParts.get(4);
        ChannelConnector.connect(request.resp, request.externalCache, channelId); // no exception => channel exists
        String testCollectionId = request.uriParts.get(5);
        String testId = request.uriParts.get(6);
        if (testCollectionId.isEmpty() || testId.isEmpty()) {
            request.badRequest();
            return;
        }
        String test = testCollectionId + "/" + testId;
        if ("null".equals(testId)) {
            request.getSession().removeAttribute(NEXT_CLIENT_TEST);
            log.info("Clear client test");

        } else {
            request.getSession().setAttribute(NEXT_CLIENT_TEST, test);
            log.info("Client test is " + test);
        }
        request.ok();
    }
}
