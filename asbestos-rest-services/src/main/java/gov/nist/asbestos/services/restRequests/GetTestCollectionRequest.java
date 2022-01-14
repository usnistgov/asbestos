package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import java.util.logging.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "collection"
// 4 - testCollectionId
// return list of test names in collection
// and a flag labeling the collection as client or server

public class GetTestCollectionRequest {

    private static final String DEPENDS_ON_KEY_NAME = "DependsOn"; // TODO: Make an enum
    public static final String TC_SEPARATOR = "/";
    private final String collectionName;

    class TestCollection {
        boolean isServerTest;
        String requiredChannel = null;
        String description;
        List<String> testNames;
        /**
         * key is in testCollection[/test] format, value is in testCollection/test,...  CSV format.
         * Self-tests README:
         *      Test collection level dependsOn property does not apply for self-test.
         *      If the test collection is part of Self-tests, then testDependencies can marked for documentation purposes only.
         *      No runtime checking is possible at this time.
         *
         *      TestCollection.properties declares
         *      Collection Level
         *          DependsOn=TC/TestName (which means a specific test in test collection) or TC/ (with a trailing slash means all tests in the TC)
         *          TestLevelDependencies=TestName1,TestName2 (These are prefixed with the current test collection name
         *      in the Map Key and its retrieved values from the test.properties file are prefixed with current test collection.) TC is not allowed here.
         *
         *      Test Level
         *          DependsOn=TestName (current test collection scope) or TC/TestName or TC/
         *
         */
        Map<String, String[]> testDependencies;
    }

    private static Logger log = Logger.getLogger(GetTestCollectionRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && request.uriParts.get(3).equals("collection");
    }

    public GetTestCollectionRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        collectionName = request.uriParts.get(4);
    }

    public void run() {
        request.announce("GetTestCollection");

        TestCollection tc = new TestCollection();
        tc.description = request.ec.getTestCollectionDescription(collectionName);
        Properties props = request.ec.getTestCollectionProperties(collectionName);
        tc.isServerTest = !"client".equals(props.getProperty("TestType"));
        tc.testNames = request.ec.getTestsInCollection(collectionName);
        tc.requiredChannel = props.getProperty("Channel");

        String dependsOnValue = props.getProperty(DEPENDS_ON_KEY_NAME);
        Map<String, String[]> tempMap = loadTestCollectionDependencies(dependsOnValue);
        if (tempMap != null) {
           tc.testDependencies = tempMap;
        }

        final String mapKeys = props.getProperty("TestLevelDependsOnMapKeys");
        tempMap = loadTestLevelDependencies(mapKeys);
        if (tempMap != null) {
            if (tc.testDependencies != null) {
                tc.testDependencies.putAll(tempMap);
            } else {
                tc.testDependencies = tempMap;
            }
        }

        request.returnObject(tc);
        request.ok();
    }

    private Map<String, String[]> loadTestCollectionDependencies(String testArtifactIds) {
        if (testArtifactIds != null && !"".equals(testArtifactIds)) {
            List<String> dependsOnList = Arrays.asList(testArtifactIds.split(","));
            List<String> list = dependsOnList
                    .stream()
                    .map(s -> s.trim())
                    .filter(s -> s.contains(TC_SEPARATOR))
                    .collect(Collectors.toList());
            if (list != null) {
                Map<String, String[]> map = new HashMap<>();
                map.put(collectionName, list.toArray(new String[list.size()]));
                return map;
            }
        }
        return null;
    }

    private String tcPrefix(String testName) {
        return String.format("%s%s%s", collectionName, TC_SEPARATOR, testName);
    }

    private Map<String, String[]> loadTestLevelDependencies(String values) {
        Map<String, String[]> map = new HashMap<>();
        if (values != null && values.trim().length() > 0) {
            String[] testLevelDependsOnMapKeys = values.split(",");
            for (String testName : testLevelDependsOnMapKeys) {
                String testNameTrimmed = testName.trim();
                Properties props = request.ec.getTestProperties(collectionName, testNameTrimmed);
                if (props != null) {
                    String dependsOnValue = props.getProperty(DEPENDS_ON_KEY_NAME);
                    List<String> list = getTestLevelPrefixedDependsOnList(dependsOnValue);
                    if (list != null) {
                        map.put(tcPrefix(testNameTrimmed), list.toArray(new String[list.size()]));
                    }
                } else {
                    log.severe(String.format("%s/%s was defined, but %s/%s was not found.", collectionName, EC.TEST_COLLECTION_PROPERTIES, testNameTrimmed, EC.TEST_PROPERTIES));
                }
            }
            return map;
        }
        return null;
    }

    private List<String> getTestLevelPrefixedDependsOnList(String testArtifactIds) {
        if (testArtifactIds != null && !"".equals(testArtifactIds)) {
            List<String> dependsOnList = Arrays.asList(testArtifactIds.split(","));
            return dependsOnList
                    .stream()
                    .map(s -> s.contains(TC_SEPARATOR) ? s.trim() : tcPrefix(s.trim()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
