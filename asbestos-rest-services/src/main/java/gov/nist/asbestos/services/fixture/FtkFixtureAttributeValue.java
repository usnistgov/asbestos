package gov.nist.asbestos.services.fixture;

import java.util.Map;
import java.util.UUID;

public class FtkFixtureAttributeValue extends FixturePlaceholderReplacer {
    private static final String PLACEHOLDER_BEGIN = "@{";
    private static final String PLACEHOLDER_END = "}";

    @Override
    String getBeginText() {
        return PLACEHOLDER_BEGIN;
    }

    @Override
    String getEndText() {
        return PLACEHOLDER_END;
    }

    @Override
    String getReplacementText(String placeholderName, Map<String, String> paramsMap) throws Exception {
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
