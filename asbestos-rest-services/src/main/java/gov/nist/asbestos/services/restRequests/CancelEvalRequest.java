package gov.nist.asbestos.services.restRequests;

// Client Testing - un-schedule testCollection/testId for testSession/channel
// 0 - empty
// 1 - app context
// 2 - "canceleval"
// 3   testSession__channelId
// Cancel pending evaluation (client test)
// payload is ignored

import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.Request;
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
        request.announce("CancelEval");
        String channelId = request.uriParts.get(3);
        ChannelConnector.connect(request.resp, request.externalCache, channelId); // no exception => channel exists
        request.getSession().removeAttribute(EvalRequest.NEXT_CLIENT_TEST);
        request.ok();
    }
}
