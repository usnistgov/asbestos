package gov.nist.asbestos.asbestosProxy.requests;

// Client Testing - schedule testCollection/testId for testSession/channel
// 0 - empty
// 1 - app context
// 2 - "canceleval"
// 3   testSession__channelId
// Prepare the channel for an evaluation (client test)
// payload is ignored

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import org.apache.log4j.Logger;

public class CancelEvalRequest {
    private static Logger log = Logger.getLogger(CancelEvalRequest.class);
    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(2).equals("canceleval");
    }

    public CancelEvalRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("CancelEval");
        String channelId = request.uriParts.get(3);
        ChannelConnector.connect(request.resp, request.externalCache, channelId); // no exception => channel exists
        request.getSession().removeAttribute(EvalRequest.NEXT_CLIENT_TEST);
        request.resp.setStatus(request.resp.SC_OK);
    }
}
