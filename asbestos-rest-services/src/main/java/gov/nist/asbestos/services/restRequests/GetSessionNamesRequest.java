package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "rw"
// 3 - "testSession"
// Return list of TestSession IDs

public class GetSessionNamesRequest {
    private static Logger log = Logger.getLogger(GetSessionNamesRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "testSession".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetSessionNamesRequest(Request request) {
        this.request = request;
    }

    public void run()  {
        request.announce("GetSessionNames");
        List<String> names = getSessionNames(request.externalCache);
        request.returnList(names);
        request.ok();
    }

    public static List<String> getSessionNames(File externalCache) {
        return Dirs.dirListingAsStringList(EC.ftkSessionsDir(externalCache));
    }
}
