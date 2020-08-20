package gov.nist.asbestos.asbestosProxy.requests;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "document"
// 4 - id

import gov.nist.asbestos.client.Base.DocumentCache;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;

import java.io.IOException;

public class GetDocumentRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 &&
                "log".equalsIgnoreCase(request.uriParts.get(2)) &&
                "document".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetDocumentRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetDocumentRequest");

        String id = request.uriParts.get(4);

        DocumentCache docuementCache = new DocumentCache(request.ec);

        byte[] contents = docuementCache.getDocumentFromCache(id);
        String mimeType = docuementCache.getDocumentTypeFromCache(id);
        request.resp.setHeader("content-type", mimeType);
        request.resp.getOutputStream().write(contents);
        request.ok();
    }

    static public String getURL(String id) {
        ServiceProperties serviceProperties = ServiceProperties.getInstance();
        return "http://" +
                serviceProperties.getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) +
                "/log/document/" +
                id;
    }
}
