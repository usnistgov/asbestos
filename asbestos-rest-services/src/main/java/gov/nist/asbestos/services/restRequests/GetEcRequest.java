package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ec"
// [4] - "testCollectionProperties" - Optional
// [5] - testCollectoinName - Required if uriPart 4 is present

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.client.Base.Request;

import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class GetEcRequest {
    private static final Logger logger = Logger.getLogger(GetEcRequest.class.getName());

    public static boolean isRequest(Request request) throws Exception {
        Objects.requireNonNull(request);
        throw new Exception("Unsupported method, use runRequest.");
    }

    public static boolean runRequest(Request request) {
        Objects.requireNonNull(request);
        request.setType(GetEcRequest.class.getSimpleName());
        final int size = request.uriParts.size();
        if (size > 3
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "ec".equalsIgnoreCase(request.uriParts.get(3))
        ) {
            switch (size) {
                case 4: doEcPath(request); return true;
                case 6: if ("testCollectionProperties".equals(request.uriParts.get(4))) doTcProps(request, request.uriParts.get(5)); return true;
                default: break;
            }
        }
        return false;
    }


   private static void doEcPath(Request request) {
        try {
            request.resp.setContentType("text/plain");
            request.resp.getOutputStream().print(request.externalCache.toString());
        }catch (Exception ioex) {
            logger.warning(ioex.toString());
        }
    }

    private static void doTcProps(Request request, String testCollectionName) {
        try {
            Properties p = request.ec.getTestCollectionProperties(testCollectionName);
            ObjectMapper objectMapper = new ObjectMapper();
            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(objectMapper.writeValueAsString(p));
        } catch (Exception ex) {
            logger.warning(ex.toString());
        }

    }


}
