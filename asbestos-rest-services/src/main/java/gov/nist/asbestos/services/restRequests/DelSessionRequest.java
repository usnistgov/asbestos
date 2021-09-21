package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gov.nist.asbestos.services.restRequests.GetSessionNamesRequest.getSessionNames;
// 0 - empty
// 1 - app context
// 2 - "rw" or "accessGuard"
// 3 - "testSession"
// 4 - session name
// Return list of TestSession IDs

public class DelSessionRequest {
    private static Logger log = Logger.getLogger(DelSessionRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 5) {
            String testSessionName = request.uriParts.get(4);
            return "testSession".equalsIgnoreCase(request.uriParts.get(3)) && SimStore.isValidCharsPattern().matcher(testSessionName).matches();
        }
        return false;
    }

    public DelSessionRequest(Request request) {
        this.request = request;
    }

    public void run() {
        String theSessionToDelete = request.uriParts.get(4);
        try {
            request.announce("DelSession");
            File channels = EC.ftkChannelsDir(request.externalCache);
            File tsChannels = new File(channels, theSessionToDelete);

            if (Dirs.listOfFiles(tsChannels).size() > 0) {
                LocalDateTime dtm = LocalDateTime.now();
                DateTimeFormatter dttmfmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String dttmString = dtm.format(dttmfmt);

                File dstFile = new File(EC.ftkTrashCan(request.externalCache), String.format("deletedSessionChannels_%s_%s", theSessionToDelete, dttmString));
                Dirs.softDelete(tsChannels, dstFile);
            } else {
                Dirs.deleteDir(tsChannels);
            }

            File sessionDir = EC.ftkSessionDir(request.externalCache, theSessionToDelete);
            Dirs.deleteDir(sessionDir);

            List<String> names = getSessionNames(request.externalCache);
            request.returnList(names);
            request.ok();
        } catch (IOException ex) {
            log.error("DelSessionRequest could not remove " + theSessionToDelete + ". Please remove the files manually if possible.");
            log.error(ex);
        }
    }
}
