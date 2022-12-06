package gov.nist.asbestos.services.fixture;

import java.util.UUID;

public class SimpleFixturePlaceholderReplacer extends FixturePlaceholderReplacer {
    private static final String PLACEHOLDER_BEGIN = "@{";
    private static final String PLACEHOLDER_END = "}";

    @Override
    public String getBeginText() {
        return PLACEHOLDER_BEGIN;
    }

    @Override
    public String getEndText() {
        return PLACEHOLDER_END;
    }

    public SimpleFixturePlaceholderReplacer(FixturePartsLoader fixturePartsLoader) {
        super(fixturePartsLoader);
    }

    @Override
    public String getReplacementText(String placeholderName) throws Exception {
        AttributeValuePlaceholderEnum placeholderEnum;
        try {
            placeholderEnum = AttributeValuePlaceholderEnum.valueOf(placeholderName);
        } catch (IllegalArgumentException iaex) {
            throw new Exception(String.format("Do not understand fixture placeholder attribute name %s.", placeholderName));
        }

        switch (placeholderEnum) {
            case RandomUUID:
                return UUID.randomUUID().toString();
            default:
                throw new Exception("Unhandled case: " + placeholderEnum.name());
        }

    }
}
