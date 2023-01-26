package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.testcollection.VARIABLE_PROP_REFERENCE_PARTS;

import java.util.Arrays;
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
            String codeParts[] = codedValuePart.split(ASBESTOS_PROPERTY_VALUE_CODE_DELIMITER, VARIABLE_PROP_REFERENCE_PARTS.values().length);
            int codePartsLength = codeParts.length;
            final long minExpectedCount = VARIABLE_PROP_REFERENCE_PARTS.minimumExpectedLength();  // Arrays.asList( VARIABLE_PROP_REFERENCE_PARTS.values() ).stream().filter(s -> s.isRequired()).count();
            if (codePartsLength < minExpectedCount) {
                log.severe(String.format("PropertyReference codeParts length of %d less than %d.", codePartsLength, minExpectedCount));
                return null;
            }
            if (EC.TEST_COLLECTION_PROPERTIES.equals(codeParts[VARIABLE_PROP_REFERENCE_PARTS.File.ordinal()])) {
                String propValue = tcProperties.getProperty(codeParts[VARIABLE_PROP_REFERENCE_PARTS.Property.ordinal()]);
                if (propValue == null) {
                    boolean defaultToGlobalServiceProperty = VARIABLE_PROP_REFERENCE_PARTS.DefaultToGlobalServiceProperty.name().equals( codeParts[VARIABLE_PROP_REFERENCE_PARTS.DefaultToGlobalServiceProperty.ordinal()] );
                    if (defaultToGlobalServiceProperty ) {
                        return replaceTheString(value, codedValuePart, VARIABLE_PROP_REFERENCE_PARTS.DefaultToGlobalServiceProperty.getToken());
                    }
                    String errorMsg = String.format("Null property %s in %s", codeParts[VARIABLE_PROP_REFERENCE_PARTS.Property.ordinal()], EC.TEST_COLLECTION_PROPERTIES);
                    log.severe(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                return replaceTheString(value, codedValuePart, propValue);
            }
        }
        return value;
    }

    private static String replaceTheString(String value, String codedValuePart, String s2) {
        return value.replaceFirst(
                Pattern.quote(String.format("%s%s%s", BEGIN_ASBESTOS_PROPERTY_VALUE_VARIABLE, codedValuePart, END_ASBESTOS_PROPERTY_VALUE_VARIABLE)),
                s2);
    }
}
