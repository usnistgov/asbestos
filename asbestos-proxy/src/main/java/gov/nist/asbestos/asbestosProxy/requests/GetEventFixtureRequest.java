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
import gov.nist.asbestos.client.Base.Returns;
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

    static class Model {
        Request request;
        String testCollectionId;
        String testId;
        String fhirPath;
        String fixturePath;

        Model(Request request) {
            this.request = request;
            testCollectionId = request.uriParts.get(4);
            testId = request.uriParts.get(5);
            fhirPath = request.getParm("fhirPath");
            fixturePath = request.getParm("url");
        }
    }

    Model model;

    public GetEventFixtureRequest(Request request) {
        model = new Model(request);
    }

    public void run() throws IOException {
        request.announce("GetEventFixtureRequest");

        URL url = request.getFullUrl();
        if (url == null)
            throw new RuntimeException("url is a required parameter");

        ResourceWrapper wrapper = request.ec.getStaticFixture(model.testCollectionId, model.testId, request.channelId, model.fixturePath, model.fhirPath, url);
        if (wrapper == null)
            throw new RuntimeException("Cannot find content");
        UIEvent uiEvent = new UIEvent(request.ec).fromResource(wrapper);
        Returns.returnObject(request.resp, uiEvent);

//        String json = new Gson().toJson(uiEvent);
//        request.resp.setContentType("application/json");
//        request.resp.getOutputStream().write(json.getBytes());
//        request.ok();
    }
}
