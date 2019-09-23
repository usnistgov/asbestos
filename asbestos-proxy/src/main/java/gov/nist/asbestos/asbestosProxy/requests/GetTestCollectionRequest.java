package gov.nist.asbestos.asbestosProxy.requests;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Properties;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "collection"
// 4 - testCollectionId
// return list of test names in collection

public class GetTestCollectionRequest {

    class TestCollection {
        boolean isServerTest;
        List<String> testNames;
    }

    private static Logger log = Logger.getLogger(GetTestCollectionRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && request.uriParts.get(3).equals("collection");
    }

    public GetTestCollectionRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetTestCollection");
        String collectionName = request.uriParts.get(4);

        TestCollection tc = new TestCollection();

        Properties props = request.ec.getTestCollectionProperties(collectionName);
        tc.isServerTest = !"client".equals(props.getProperty("TestType"));
        tc.testNames = request.ec.getTestsInCollection(collectionName);

        Returns.returnObject(request.resp, tc);
        log.info("OK");
    }
}
