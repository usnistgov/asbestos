package gov.nist.asbestos.services.fixture;

import gov.nist.asbestos.client.Base.EC;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;

public class FixturePartsLoader {
    private static Logger log = Logger.getLogger(FixturePartsLoader.class.getName());

    EC ec;
    String testCollection;
    String testName;
    Map<String, String> paramsMap;


    public FixturePartsLoader(EC ec, String testCollection, String testName, Map<String, String> paramsMap) {
        this.ec = ec;
        this.testCollection = testCollection;
        this.testName = testName;
        this.paramsMap = paramsMap;
    }

    public String loadFixture(String fixtureId, String resourceType) throws Exception {

        if (resourceType == null) {
            resourceType = "Bundle";
        }

        File fixtureFile = getFixtureFile(fixtureId, getTestDirectory(testCollection, testName), resourceType);
        if (! fixtureFile.exists()) {
            // Try Common
            fixtureFile = getFixtureFile(fixtureId, getTestDirectory(testCollection, testName), "..".concat(File.separator).concat("Common"));
            if (! fixtureFile.exists()) {

                // Try base Test Collection if the optional parameter is available
                String baseTestCollection = paramsMap.get(FixturePlaceholderParamEnum.baseTestCollection.name());
                if (isSafeFileName(baseTestCollection)) {
                    String baseTestCollectionNameDecoded = URLDecoder.decode(baseTestCollection, StandardCharsets.UTF_8.toString());
                    String baseTestName = paramsMap.get(FixturePlaceholderParamEnum.baseTestName.name());
                    if (baseTestName == null) {
                        fixtureFile = getFixtureFile(fixtureId, getTestDirectory(baseTestCollectionNameDecoded, testName), resourceType);
                    } else if (isSafeFileName(baseTestName)) {
                        // Try baseTestName
                        String baseTestNameDecoded = URLDecoder.decode(baseTestName, StandardCharsets.UTF_8.toString());
                        fixtureFile = getFixtureFile(fixtureId, getTestDirectory(baseTestCollectionNameDecoded, baseTestNameDecoded), resourceType);
                    }
                }
            }
        }
        if (! fixtureFile.exists()) {
            String errorMessage = "fixtureFile is not valid or does not exist: " + fixtureFile.toString();
            log.severe(errorMessage);
            throw new Exception(errorMessage);
        }
        return new String(Files.readAllBytes(fixtureFile.toPath()));
    }

    File getTestDirectory(String testCollection, String testName) {
        return ec.getTest(testCollection, testName);
    }

    private File getFixtureFile(String fixtureId, File testDir, String location) {
        return new File(testDir, location.concat(File.separator.concat(appendXmlFileExtension(fixtureId))));
    }

    private static String appendXmlFileExtension(String fileName) {
        return fileName.concat(".xml");
    }

    /**
     * Mainly used to avoid unwanted path traversal using ..\
     * @param name
     * @return
     */
    public static boolean isSafeFileName(String name) {
        return name != null && !"".equals(name) && !name.contains(".");
    }

}
