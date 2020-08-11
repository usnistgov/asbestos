package gov.nist.asbestos.asbestosProxy.requests;

// 0 - empty
// 1 - app context
// 2 - "static"
// 3 - testCollectionId
// 4 - testId
// 5 - resourceType
// 6 - fileName


import gov.nist.asbestos.client.Base.FhirSearchPath;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;

public class GetStaticResourceRequest {
    private static Logger log = Logger.getLogger(GetStaticResourceRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7;
    }

    public GetStaticResourceRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetStaticResourceRequest");
        String testCollectionId = request.uriParts.get(3);
        String testId = request.uriParts.get(4);
        String resourceType = request.uriParts.get(5);
        String fileName = request.uriParts.get(6);
        URL url = request.getFullUrl();

        File testDef = request.ec.getTest(testCollectionId, testId);

        FhirClient fhirClient = FhirSearchPath.getFhirClient(request.ec, testDef, request.channelId);

        ResourceWrapper wrapper = request.ec.getStaticFixture(
                testCollectionId,
                testId,
                request.channelId,
                resourceType + "/" + fileName,
                null,
                url);

        Returns.returnResource(request.resp, wrapper.getResource());
    }
}
