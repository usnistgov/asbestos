package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ec"
// [4] - "testCollectionProperties" - Optional

import gov.nist.asbestos.client.Base.Request;

import java.io.IOException;
import java.util.logging.Logger;

public class GetEcRequest {
    private Request request;
    private Runnable runnable;
    private static final Logger logger = Logger.getLogger(GetEcRequest.class.getName());

    public static boolean isRequest(Request request) {
        try {
            getMode(request);
            return true;
        } catch (Exception ex) {
            logger.warning(ex.toString());
        }
        return false;
    }

    private static Class<? extends Runnable> getMode(Request request) throws Exception {
        final int size = request.uriParts.size();
        if (size > 3
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "ec".equalsIgnoreCase(request.uriParts.get(3))
        ) {
            switch (size) {
                case 4: return EcRunner.class;
                case 5: if ("testCollectionProperties".equals(request.uriParts.get(4))) return EcRunner.class;
                default: break;
            }
        }
        throw new Exception("GetEcRequest - Unknown request");
    }


    class EcRunner implements Runnable {
        Request request;
        public EcRunner(Request request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                request.resp.setContentType("text/plain");
                request.resp.getOutputStream().print(request.externalCache.toString());
            }catch (IOException ioex) {
                logger.warning(ioex.toString());
            }
        }
    }

    public GetEcRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws Exception {
        getMode(request).newInstance().run();
    }
}
