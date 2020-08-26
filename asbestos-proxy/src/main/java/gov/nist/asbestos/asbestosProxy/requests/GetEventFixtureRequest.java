package gov.nist.asbestos.asbestosProxy.requests;

// This returns static fixture to UI as UIEvent
// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "eventFixture"
// 4 - testCollectionId
// 5 - testId
// 6 - resourceType
// param url=relative path to file
// optional param fhirPath=path within bundle
// returns UIEvent as JSON

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;

public class GetEventFixtureRequest {
    private static Logger log = Logger.getLogger(GetEventFixtureRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("eventFixture");
    }

    public GetEventFixtureRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetEventFixtureRequest");

        String testCollectionId = request.uriParts.get(4);
        String testId = request.uriParts.get(5);
        String fixturePath = request.getParm("url");
        URL url = request.getFullUrl();
        if (url == null)
            throw new RuntimeException("url is a required parameter");
        String fhirPath = request.getParm("fhirPath");
        ResourceWrapper wrapper = request.ec.getStaticFixture(testCollectionId, testId, request.channelId, fixturePath, fhirPath, url);
        if (wrapper == null)
            throw new RuntimeException("Cannot find content");
        UIEvent uiEvent = new UIEvent(request.ec).fromResource(wrapper);

        String json = new Gson().toJson(uiEvent);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().write(json.getBytes());
        request.ok();
    }
}
