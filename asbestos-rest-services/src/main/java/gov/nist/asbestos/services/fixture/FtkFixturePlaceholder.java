package gov.nist.asbestos.services.fixture;

import gov.nist.asbestos.client.Base.EC;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class FtkFixturePlaceholder extends FixturePlaceholderReplacer {
    private static final String PLACEHOLDER_BEGIN = "<!--@{";
    private static final String PLACEHOLDER_END = "}-->";
    String testCollection;
    String testName;
    EC ec;

    public FtkFixturePlaceholder(EC ec, String testCollection, String testName) {
        this.ec = ec;
        this.testCollection = testCollection;
        this.testName = testName;
    }

    @Override
    String getBeginText() {
        return PLACEHOLDER_BEGIN;
    }

    @Override
    String getEndText() {
        return PLACEHOLDER_END;
    }

    @Override
    protected String getReplacementText(String placeholderName, final Map<String, String> paramsMap) throws Exception {
        FixturePlaceholderEnum placeholderEnum;
        try {
            placeholderEnum = FixturePlaceholderEnum.valueOf(placeholderName);
        } catch (IllegalArgumentException iaex) {
            throw new Exception(String.format("Do not understand fixture placeholder name %s.", placeholderName));
        }

        // Expect a file name same name as the placeholder
        // Example  @{BundleMetaProfileElement} = BundleMetaProfileElement.xml
        if (isSafeFileName(placeholderEnum.name())) {
            File testDir = ec.getTest(testCollection, testName);
            if (testDir == null || !testDir.exists() || !testDir.isDirectory()) {
                throw new Exception(String.format("TestId not found: %s/%s.", testCollection, testName));
            }

            return loadFixture(placeholderEnum.name(), null, paramsMap);
        } else {
            throw new Exception(String.format("%s is not safe", placeholderEnum.name()));
        }

    }




    private File getTestDirectory(String testCollection, String testName) {
        File testDir = request.ec.getTest(testCollection, testName);
                if (testDir == null || !testDir.exists() || !testDir.isDirectory()) {
                        unexpectedMessage(String.format("TestId not found: %s/%s.", testCollection, testName));
                        return null;
                    }
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
    private static boolean isSafeFileName(String name) {
        return name != null && !"".equals(name) && !name.contains(".");
    }

    public File getTestDirectory() {
        return testDirectory;
    }
}
