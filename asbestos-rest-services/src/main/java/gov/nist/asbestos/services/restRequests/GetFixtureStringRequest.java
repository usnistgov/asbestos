package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.fixture.FixturePlaceholderEnum;
import gov.nist.asbestos.fixture.FixturePlaceholderParamEnum;

import java.util.UUID;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 0 - empty
// 1 - web server application context
// 2 - "engine"
// 3 - "getFixtureString"
// 4 - channelId
// 5 - testCollectionId
// 6 - testId
// Example: https://fhirtoolkit.test:9743/asbestos/engine/getFixtureString/default__limited/MHD_DocumentRecipient_minimal/Missing_DocumentManifest
//  ?fixtureId=pdb&[baseTestCollection=&baseTestName=]&[fixtureElementPlaceholder=LocalFixtureReferenceFileName]&[resourceType=DirectoryName]
// See @FixturePlaceholderParamEnum for complete search order reference.
// fixtureId is searched for in the current test Collection, test name, Bundle,
// if not found, the fall back location is ../../Common[Test.properties:hidden=true].
// If LocalFixtureReferenceFileName (without the file extension) is not found in the current Test Bundle directory,
// then the fall back search location is CurrentTestCollection\Common[Test.properties:hidden=true]\Name

public class GetFixtureStringRequest {
    public static final String PLACEHOLDER_BEGIN = "@{";
    public static final String PLACEHOLDER_END = "}";
    private static Logger log = Logger.getLogger(GetFixtureStringRequest.class.getName());

    private Request request;
    private String testCollection;
    private String testName;

    public GetFixtureStringRequest(Request request) throws IOException {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        this.testCollection = request.uriParts.get(5);
        this.testName = URLDecoder.decode(request.uriParts.get(6), StandardCharsets.UTF_8.toString());
    }

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 7) {
            String uriPart3 = request.uriParts.get(3);
            return "getFixtureString".equals(uriPart3);
        }
        return false;
    }

    public void run() throws IOException {
        request.announce("GetFixtureStringRequest");

        // Read fixtureId File
        Map<String, String> paramsMap = request.getParametersMap();

        String fixtureId = paramsMap.get(FixturePlaceholderParamEnum.fixtureId.name());
        if (! isSafeFileName(fixtureId)) {
            String message = FixturePlaceholderParamEnum.fixtureId.name() + " parameter is not valid.";
            unexpectedMessage(message);
            return;
        }

        String resourceDirectory = paramsMap.get(FixturePlaceholderParamEnum.resourceType.name());
        if (resourceDirectory != null) {
            if (!isSafeFileName(resourceDirectory)) {
                String message = FixturePlaceholderParamEnum.resourceType.name() + " parameter is not valid.";
                unexpectedMessage(message);
                return;
            }
        }

        String fixtureString =  readFixtureString(testCollection, testName, fixtureId, resourceDirectory);
        if (fixtureString == null) {
            // Try base Test Collection if the optional parameter is available
            String baseTestCollection = paramsMap.get(FixturePlaceholderParamEnum.baseTestCollection.name());
            if (isSafeFileName(baseTestCollection)) {
                String baseTestCollectionNameDecoded = URLDecoder.decode(baseTestCollection, StandardCharsets.UTF_8.toString());
                String baseTestName = paramsMap.get(FixturePlaceholderParamEnum.baseTestName.name());
                if (baseTestName == null) {
                    fixtureString = readFixtureString(baseTestCollectionNameDecoded, testName, fixtureId, resourceDirectory);
                } else if (isSafeFileName(baseTestName)) {
                    // Try baseTestName
                    String baseTestNameDecoded = URLDecoder.decode(baseTestName, StandardCharsets.UTF_8.toString());
                    fixtureString = readFixtureString(baseTestCollectionNameDecoded, baseTestNameDecoded, fixtureId, resourceDirectory);
                }
            }
        }

        if (fixtureString != null) {
            fixtureString = replacePlaceholder(fixtureString, paramsMap);
            if (fixtureString != null) {
                try {
                    // Parse to see if the whole thing is a parsable Fixture
                    Format outFormat = Format.JSON; // TODO: this should honor the TestScript Operation contentType value
                    String jsonStr = ParserBase.encode(ParserBase.parse(fixtureString, Format.XML), outFormat);
                    Returns.returnString(request.resp, jsonStr);
                    return;
                } catch (Exception ex) {
                    unexpectedMessage("ParserBase Exception: " + ex.toString());
                    return;
                }
            } else {
                unexpectedMessage("replacePlaceholder is null.");
                return;
            }
        } else {
            String message = "fixtureString is null.";
            log.severe(message);
            unexpectedMessage(message);
            return;
        }

    }

    private String readFixtureString(String testCollection, String testName, String fixtureId, String resourceDirectory) throws IOException {
        File testDir = request.ec.getTest(testCollection, testName);
        if (testDir == null || !testDir.exists() || !testDir.isDirectory()) {
            unexpectedMessage(String.format("TestId not found: %s/%s.", testCollection, testName));
            return null;
        }

        if (resourceDirectory == null) {
            resourceDirectory = "Bundle";
        }

        File fixtureFile = getFixtureFile(fixtureId, testDir, resourceDirectory);
        if (! fixtureFile.exists()) {
            // Try Common
            fixtureFile = getFixtureFile(fixtureId, testDir, "..".concat(File.separator).concat("Common"));
            if (! fixtureFile.exists()) {
                return null;
            }
        }

        return new String(Files.readAllBytes(fixtureFile.toPath()));
    }

    @NotNull
    private File getFixtureFile(String fixtureId, File testDir, String location) {
        return new File(testDir, location.concat(File.separator.concat(appendXmlFileExtension(fixtureId))));
    }

    private void unexpectedMessage(String message) throws IOException {
        log.warning("GetFixtureStringRequest unexpectedMessage: " + message);
        Returns.returnPlainTextResponse(request.resp, 400, String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<OperationOutcome xmlns=\"http://hl7.org/fhir\">\n" +
                "  <id value=\"exception\"/> \n" +
                "  <text> \n" +
                "    <status value=\"generated\"/> \n" +
                "    <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "      <p>%s</p> \n" +
                "    </div> \n" +
                "  </text> \n" +
                "  <issue> \n" +
                "    <severity value=\"error\"/> \n" +
                "    <code value=\"exception\"/> \n" +
                "    <details> \n" +
                "      <text value=\"Internal exception.\"/> \n" +
                "    </details> \n" +
                "  </issue> \n" +
                "</OperationOutcome> ",message));
    }

    /**
     * Mainly used to avoid unwanted path traversal using ..\
     * @param name
     * @return
     */
    private static boolean isSafeFileName(String name) {
        return name != null && !"".equals(name) && !name.contains(".");
    }


    private String replacePlaceholder(String fixtureString, final Map<String, String> paramsMap) throws IOException {
        int from =  fixtureString.indexOf(PLACEHOLDER_BEGIN);
        if (from == -1) {
            // Done, no more placeholders exist to replace
            return fixtureString;
        }
        int to = fixtureString.indexOf(PLACEHOLDER_END, from);
        if (to == -1) {
           unexpectedMessage(String.format("Placeholder at %d has no closing.", from));
           return null;
        }
        String placeholderName = fixtureString.substring(from+PLACEHOLDER_BEGIN.length(), to);
        FixturePlaceholderEnum placeholderEnum = null;
        try {
            placeholderEnum = FixturePlaceholderEnum.valueOf(placeholderName);
        } catch (IllegalArgumentException iaex) {
            unexpectedMessage(String.format("%s fixture placeholder is not a registered enumeration type.", placeholderName));
            return null;
        }

        String paramValue = paramsMap.get(placeholderName);
        String placeholderFixtureString = null;
        if (paramValue == null) {
            if (FixturePlaceholderEnum.RandomUUID.equals(placeholderEnum)) {
                paramValue = FixturePlaceholderEnum.RandomUUID.toString();
                placeholderFixtureString = UUID.randomUUID().toString();
            }
            else {
                // If null, expect a file name same name as the placeholder
                // Example  @{BundleMetaProfileElement} = BundleMetaProfileElement.xml
                paramValue = placeholderName;
                if (isSafeFileName(paramValue)) {
                    placeholderFixtureString = readFixtureString(testCollection, testName, paramValue, null);
                } else {
                    log.severe(String.format("%s is not safe",paramValue));
                    return null;
                }
            }
        if (placeholderFixtureString != null) {
            return replacePlaceholder(
                    fixtureString.replaceAll(
                            Pattern.quote(String.format("%s%s%s", PLACEHOLDER_BEGIN, placeholderName, PLACEHOLDER_END)), Matcher.quoteReplacement(placeholderFixtureString)), paramsMap);
        } else {
            unexpectedMessage(String.format("%s fixture string value is null", paramValue));
            return null;
        }
     }
        unexpectedMessage(String.format("unresolved placeholder %s", placeholderName));
        return null;
    }

    private static String appendXmlFileExtension(String fileName) {
       return fileName.concat(".xml");
    }
}
