package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "collections"
// Return list of test collection names

public class GetTestCollectionNamesRequest {
    private static Logger log = Logger.getLogger(GetTestCollectionNamesRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "collections".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetTestCollectionNamesRequest(Request request) {
        this.request = request;
    }

    class Collection {
        String name;
        boolean server;
        String channel;
        boolean hidden;
    }

    public void run() throws IOException {
        request.announce("GetTestCollectionNames");
        List<Collection> collections = new ArrayList<>();
        List<String> names = request.ec.getTestCollectionNames();
        for (String name : names) {
            Collection collection = new Collection();
            Properties props = request.ec.getTestCollectionProperties(name);
            collection.server = "server".equals(props.getProperty("TestType"));
            collection.name = name;
            collection.channel = props.getProperty("channel");
            collection.hidden = "true".equals(props.getProperty(EC.HIDDEN_KEY_NAME));
            collections.add(collection);
        }
        String json = new Gson().toJson(collections);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
