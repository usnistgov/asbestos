package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.EC;
import java.util.logging.Logger;

import gov.nist.asbestos.testcollection.COMPONENT_PROP_REFERENCE_PARTS;
import org.hl7.fhir.r4.model.TestScript;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class AsbestosComponentPath {
    private static Logger log = Logger.getLogger(AsbestosComponentPath.class.getName());
    private final static String BEGIN_ASBESTOS_COMPONENT_VALUE_VARIABLE = "#{";
    private final static String END_COMPONENT_VALUE_VARIABLE = "}";
    private final static String COMPONENT_VALUE_CODE_DELIMITER = ":";

    /**
     *
     * Expected string convention: #{script:TestCollection.properties:propertyKey}
     * Expected TestScript variable = propKey, with a defaultValue containing the relative path, without any single quotes
     * This method pulls the defaultValue
     * @param tcProperties
     * @param variableComponentList
     * @param componentExtensionValue
     * @return
     */
    public static ComponentPathValue getRelativeComponentPath(final Properties tcProperties, List<TestScript.TestScriptVariableComponent> variableComponentList, final String componentExtensionValue) {
        if (componentExtensionValue.startsWith(BEGIN_ASBESTOS_COMPONENT_VALUE_VARIABLE)) {
            int from = 0;
            int to = componentExtensionValue.indexOf(END_COMPONENT_VALUE_VARIABLE, from);
            if (to == -1) {
                log.severe(String.format("componentExtensionValue at %d has no closing.", from));
                return null;
            }
            String codedValuePart = componentExtensionValue.substring(from+BEGIN_ASBESTOS_COMPONENT_VALUE_VARIABLE.length(), to);
            String codeParts[] = codedValuePart.split(COMPONENT_VALUE_CODE_DELIMITER);
            int codePartsLength =  codeParts.length;
            final int expectedCount = COMPONENT_PROP_REFERENCE_PARTS.values().length;
            if (codePartsLength != expectedCount) {
                log.severe(String.format("ComponentReference codeParts length of %d is not equal to %d.", codePartsLength, expectedCount));
                return null;
            }
            if (EC.TEST_COLLECTION_PROPERTIES.equals(codeParts[COMPONENT_PROP_REFERENCE_PARTS.PropertiesFileName.ordinal()])) {
                String propKey = codeParts[COMPONENT_PROP_REFERENCE_PARTS.PropertyKey.ordinal()];
                String propValue = tcProperties.getProperty(propKey);
                if (propValue == null) {
                    log.severe("Property value not found for key: " + propKey + ". tcProperties size is: " + tcProperties.size());
                    return null;
                }
                String varNameToBe = codeParts[COMPONENT_PROP_REFERENCE_PARTS.VariablePrefix.ordinal()].concat(propValue);
                Optional<TestScript.TestScriptVariableComponent> optionalVar = variableComponentList.stream().filter(s -> varNameToBe.equals(s.getName())).findFirst();
                if (optionalVar.isPresent()) {
                    if (optionalVar.get().hasDefaultValue()) {
                        String replacedPath = optionalVar.get().getDefaultValue();
                        return new ComponentPathValue(true, replacedPath, componentExtensionValue);
                    } else {
                        log.severe("There is no default value specified for the variable name: " + varNameToBe);
                        return null;
                    }
                }
            } else {
                log.severe(String.format("codePart %s is not understood.", codeParts[1]));
                return null;
            }

        }
        return new ComponentPathValue(false, componentExtensionValue, null);
    }


}
