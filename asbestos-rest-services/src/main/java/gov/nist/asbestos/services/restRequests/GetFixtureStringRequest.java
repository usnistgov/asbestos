package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.services.fixture.FixturePartsLoader;
import gov.nist.asbestos.services.fixture.FixturePlaceholderParamEnum;
import gov.nist.asbestos.services.fixture.FixturePlaceholderReplacer;
import gov.nist.asbestos.services.fixture.SimpleFixturePlaceholderReplacer;
import gov.nist.asbestos.services.fixture.XmlPlaceholderReplacer;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static gov.nist.asbestos.services.fixture.FixturePartsLoader.isSafeFileName;

// 0 - empty
// 1 - web server application context
// 2 - "engine"
// 3 - "ftkLoadFixture"
// 4 - channelId
// 5 - testCollectionId
// 6 - testId
// Example: https://fhirtoolkit.test:9743/asbestos/engine/loadFtkFixture/default__limited/MHD_DocumentRecipient_minimal/Missing_DocumentManifest
//  ?fixtureId=pdb&[baseTestCollection=&baseTestName=]&[fixtureElementPlaceholder=LocalFixtureReferenceFileName]&[resourceType=DirectoryName]
// See @FixturePlaceholderParamEnum for complete search order reference.
// fixtureId is searched for in the current test Collection, test name, Bundle,
// if not found, the fall back location is ../../Common[Test.properties:hidden=true].
// If LocalFixtureReferenceFileName (without the file extension) is not found in the current Test Bundle directory,
// then the fall back search location is CurrentTestCollection\Common[Test.properties:hidden=true]\Name

public class GetFixtureStringRequest {
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
            return "loadFtkFixture".equals(uriPart3);
        }
        return false;
    }

    public void run() throws IOException {
        request.announce("loadFtkFixture");

        // Read fixtureId File
        Map<String, String> paramsMap = request.getParametersMap();

        String fixtureId = paramsMap.get(FixturePlaceholderParamEnum.fixtureId.name());
        if (! isSafeFileName(fixtureId)) {
            String message = FixturePlaceholderParamEnum.fixtureId.name() + " parameter is not valid.";
            unexpectedMessage(message);
            return;
        }

        String resourceType = paramsMap.get(FixturePlaceholderParamEnum.resourceType.name());
        if (resourceType != null) {
            if (!isSafeFileName(resourceType)) {
                String message = FixturePlaceholderParamEnum.resourceType.name() + " parameter is not valid.";
                unexpectedMessage(message);
                return;
            }
        }

        try {
            FixturePartsLoader fixturePartsLoader = new FixturePartsLoader(request.ec, testCollection, testName, paramsMap);
            String fixtureString  = fixturePartsLoader.loadFixture(fixtureId, resourceType);

            // TODO: make this channel specific if needed
            List<FixturePlaceholderReplacer> fixturePlaceholderReplacers = Arrays.asList(
                    new XmlPlaceholderReplacer(fixturePartsLoader),
                    new SimpleFixturePlaceholderReplacer(fixturePartsLoader));


            for (FixturePlaceholderReplacer fixturePlaceholderReplacer : fixturePlaceholderReplacers) {
                if (fixtureString != null) {
                    fixtureString = fixturePlaceholderReplacer.replacePlaceholders(fixtureString);
                } else {
                    String message = "Class: <" + fixturePlaceholderReplacer.getClass().getSimpleName() + "> returned null fixtureString." ;
                    log.severe(message);
                    unexpectedMessage(message);
                    return;
                }
            }

            if (fixtureString != null) {
                try {
                    // Parse to see if the whole thing is a parsable Fixture after fixture part replacements
                    Format outFormat = Format.JSON; // TODO: this should honor the TestScript Operation contentType value
                    BaseResource assembledResource = ParserBase.parse(fixtureString, Format.XML);
                    String jsonStr = null;
                    if (assembledResource instanceof Bundle) {
                        jsonStr = ParserBase.encode(assembledResource, outFormat);
                    } else {
                        Bundle outBundle = ParserBase.bundleWith(Arrays.asList((Resource) assembledResource));
                        jsonStr = ParserBase.encode(outBundle, outFormat);
                    }
                    Returns.returnString(request.resp, jsonStr);
                    return;
                } catch (Exception ex) {
                    unexpectedMessage("GetFixtureStringRequest ParserBase Exception: " + ex.toString());
                    return;
                }
            } else {
                unexpectedMessage("fixtureString is null.");
                return;
            }

        } catch (Exception ex) {
            log.severe(ex.toString());
            unexpectedMessage(ex.toString());
        }
        return;

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




}
