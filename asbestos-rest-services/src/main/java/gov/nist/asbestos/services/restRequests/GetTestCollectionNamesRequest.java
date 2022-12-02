package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.testcollection.TestCollectionPropertiesEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "collections"
// Return list of test collection names and its test collection details

public class GetTestCollectionNamesRequest {
    private static Logger log = Logger.getLogger(GetTestCollectionNamesRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "collections".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetTestCollectionNamesRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    class TestCollectionDetail {
        String name;
        boolean server;
        String channel;
        boolean hidden;
        String fhirIgName;
        String fhirIgVersion;
    }

    public void run() throws IOException {
        request.announce("GetTestCollectionNames");
        List<TestCollectionDetail> collections = new ArrayList<>();
        List<String> names = request.ec.getTestCollectionNames();
        for (String name : names) {
            TestCollectionDetail detail = new TestCollectionDetail();
            detail.name = name;
            Properties props = request.ec.getTestCollectionProperties(name);
            detail.server = "server".equals(props.getProperty(TestCollectionPropertiesEnum.TestType.name()));
            detail.channel = props.getProperty(TestCollectionPropertiesEnum.Channel.name());
            detail.hidden = "true".equals(props.getProperty(TestCollectionPropertiesEnum.Hidden.name()));
            detail.fhirIgName = props.getProperty(TestCollectionPropertiesEnum.FhirIgName.name());
            detail.fhirIgVersion = props.getProperty(TestCollectionPropertiesEnum.FhirIgVersion.name());
            collections.add(detail);
        }
        String json = new Gson().toJson(collections);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
