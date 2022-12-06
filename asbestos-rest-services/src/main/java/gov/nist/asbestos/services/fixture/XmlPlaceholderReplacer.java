package gov.nist.asbestos.services.fixture;

import java.io.File;
import java.util.logging.Logger;

public class XmlPlaceholderReplacer extends FixturePlaceholderReplacer {
    private static Logger log = Logger.getLogger(XmlPlaceholderReplacer.class.getName());
    private static final String PLACEHOLDER_BEGIN = "<!--@{";
    private static final String PLACEHOLDER_END = "}-->";

    public XmlPlaceholderReplacer(FixturePartsLoader fixturePartsLoader) {
        super(fixturePartsLoader);
    }

    @Override
    public String getBeginText() {
        return PLACEHOLDER_BEGIN;
    }

    @Override
    public String getEndText() {
        return PLACEHOLDER_END;
    }

    @Override
    protected String getReplacementText(String placeholderName) throws Exception {
        FixturePlaceholderEnum placeholderEnum;
        try {
            placeholderEnum = FixturePlaceholderEnum.valueOf(placeholderName);
        } catch (IllegalArgumentException iaex) {
            String errorMessage = String.format("Do not understand fixture placeholder name %s.", placeholderName);
            log.severe(errorMessage);
            throw new Exception(errorMessage);
        }

        // Expect a file name same name as the placeholder
        // Example  @{BundleMetaProfileElement} = BundleMetaProfileElement.xml
        if (fixturePartsLoader.isSafeFileName(placeholderEnum.name())) {
            File testDir = fixturePartsLoader.getTestDirectory(fixturePartsLoader.testCollection, fixturePartsLoader.testName);
            if (testDir == null || !testDir.exists() || !testDir.isDirectory()) {
                String errorMessage = String.format("TestId not found: %s/%s.", fixturePartsLoader.testCollection, fixturePartsLoader.testName);
                log.severe(errorMessage);
                throw new Exception(errorMessage);
            }

            return fixturePartsLoader.loadFixture(placeholderEnum.name(), "Bundle");
        } else {
            String errorMessage = String.format("%s is not safe", placeholderEnum.name());
            log.severe(errorMessage);
            throw new Exception(errorMessage);
        }

    }



}
