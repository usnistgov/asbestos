package gov.nist.asbestos.services.fixture;


import gov.nist.asbestos.services.restRequests.GetFixtureStringRequest;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FixturePlaceholderReplacer {
    private static Logger log = Logger.getLogger(FixturePlaceholderReplacer.class.getName());

    public abstract String getBeginText();
    public abstract String getEndText();


    public String replacePlaceholder(String fixtureString, final Map<String, String> paramsMap) throws Exception {
        String placeholderBegin = getBeginText();
        String placeholderEnd = getEndText();

        int from =  fixtureString.indexOf(placeholderBegin);
        if (from == -1) {
            // Done, no more placeholders exist to replace
            return fixtureString;
        }
        int to = fixtureString.indexOf(placeholderEnd, from);
        if (to == -1) {
            throw new Exception(String.format("Placeholder at %d has no closing.", from));
        }
        String placeholderName = fixtureString.substring(from+placeholderBegin.length(), to);

        String placeholderValue = paramsMap.get(placeholderName);
        String placeholderReplacement;
        if (placeholderValue == null) {
            placeholderReplacement = getReplacementText(placeholderName, paramsMap);
            if (placeholderReplacement == null) {
                throw new Exception(String.format("%s fixture placeholder replacement string value cannot be null.", placeholderName));
            }
        } else {
            // TODO: May need other safety checks here
            if (! placeholderValue.contains(getBeginText())) {
                placeholderReplacement = placeholderValue;
            } else {
                String errorMessage = "Safety check failed.";
                log.severe(errorMessage);
                throw new Exception(errorMessage);
            }
        }
        return replacePlaceholder(
                fixtureString.replaceAll(
                        Pattern.quote(String.format("%s%s%s", getBeginText(), placeholderName, getEndText())), Matcher.quoteReplacement(placeholderReplacement)), paramsMap);
    }

    abstract String getReplacementText(String placeholderName, Map<String, String> paramsMap) throws Exception;

    private String loadFixture(String testCollection, String testName, String fixtureId, String resourceType, final Map<String, String> paramsMap) throws Exception {

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
        return new String(Files.readAllBytes(fixtureFile.toPath()));
    }


}
