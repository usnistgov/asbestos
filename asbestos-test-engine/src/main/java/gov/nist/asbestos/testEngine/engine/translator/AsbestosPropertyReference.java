package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.EC;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class AsbestosPropertyReference {
    private static Logger log = Logger.getLogger(AsbestosPropertyReference.class.getName());
    private final static String BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE = "#{";
    private final static String END_ASBESTOS_PROPERTY_VALUE_VARIABLE = "}";
    private final static String ASBESTOS_PROPERTY_VALUE_CODE_DELIMITER = ":";

    public static String getValue(final Properties tcProperties, String value) {
        if (value.contains(BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE)) {
            int from = value.indexOf(BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE);
            int to = value.indexOf(END_ASBESTOS_PROPERTY_VALUE_VARIABLE, from);

            if (to == -1) {
                log.severe(String.format("asbestosProperty at %d has no closing.", from));
                return null;
            }

            String codedValuePart = value.substring(from+BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE.length(), to);
            String codeParts[] = codedValuePart.split(ASBESTOS_PROPERTY_VALUE_CODE_DELIMITER);
            int codePartsLength = codeParts.length;
            final int expectedCount = 2;
            if (codePartsLength != expectedCount) {
                log.severe(String.format("codeParts length of %d is not equal to %d.", codePartsLength, expectedCount));
                return null;
            }
            if (EC.TEST_COLLECTION_PROPERTIES.equals(codeParts[0])) {
                String propValue = tcProperties.getProperty(codeParts[1]);
                if (propValue == null) {
                    String errorMsg = String.format("Null property %s in %s", codeParts[1], EC.TEST_COLLECTION_PROPERTIES);
                    log.severe(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                return value.replaceFirst(
                        Pattern.quote(String.format("%s%s%s", BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE, codedValuePart, END_ASBESTOS_PROPERTY_VALUE_VARIABLE)),
                        propValue);
            }
        }
        return value;
    }
}
