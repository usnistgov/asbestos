package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.Request;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - "addSession"
// 4 - new session name
// Return list of TestSession IDs

public class AddSessionRequest {
    private static Logger log = Logger.getLogger(AddSessionRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && "addSession".equalsIgnoreCase(request.uriParts.get(3));
    }

    public AddSessionRequest(Request request) {
        this.request = request;
    }

    public void run()  {
        request.announce("AddSession");
        String newSession = request.uriParts.get(4);
        File channels = new File(request.externalCache, "FhirChannels");
        File newChannel = new File(channels, newSession);
        newChannel.mkdirs();
        List<String> names = Dirs.dirListingAsStringList(channels);
        request.returnList(names);
        request.ok();
    }
}
