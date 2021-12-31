package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context
// 2 - "static"
// 3 - "staticResource"
// 4 - testCollectionId
// 5 - testId
// 6 - resourceType
// 7 - fileName


import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import java.util.logging.Logger;

import java.net.URL;
import java.util.Map;

public class GetStaticResourceRequest {
    private static final Logger log = Logger.getLogger(GetStaticResourceRequest.class.getName());

    private final Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() >= 6
                && request.uriParts.get(3).equals("staticResource");
    }

    public GetStaticResourceRequest(Request request) {
        this.request = request;
    }

    public void run() {
        request.announce("GetStaticResourceRequest");
        String testCollectionId = request.uriParts.get(4);
        String testId = request.uriParts.get(5);
        String resourceType = request.uriParts.get(6);
        String fileName = request.uriParts.size() > 7 ? request.uriParts.get(7) : null;
        URL url = request.getFullUrl();
        Map<String, String> parmMap = new Ref(url).getParametersAsMap();
        String fixturePath = parmMap.containsKey("fixturePath") ? parmMap.get("fixturePath") : resourceType + "/" + fileName;
        String fhirPath = parmMap.getOrDefault("fhirPath", null);

        ResourceWrapper wrapper = request.ec.getStaticFixture(
                testCollectionId,
                testId,
                request.channelId,
                fixturePath,
                fhirPath,
                url);

        if (wrapper == null) {
            request.notFound();
            return;
        }
        request.returnResource(wrapper.getResource());
    }
}
