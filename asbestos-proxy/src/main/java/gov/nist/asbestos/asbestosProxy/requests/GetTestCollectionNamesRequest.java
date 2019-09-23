package gov.nist.asbestos.asbestosProxy.requests;

import org.apache.log4j.Logger;

import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "collections"
// Return list of channel IDs

public class GetTestCollectionNamesRequest {
    private static Logger log = Logger.getLogger(GetTestCollectionNamesRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "collections".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetTestCollectionNamesRequest(Request request) {
        this.request = request;
    }

    public void run()  {
        List<String> names = request.ec.getTestCollectionNames();
        Returns.returnList(request.resp, names);
        log.info("OK");
    }
}
