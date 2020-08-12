package gov.nist.asbestos.sharedObjects.debug;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class TsEnumerationCodeExtractor implements Function<Object, AssertionFieldValueDescription> {

    @Override
    public AssertionFieldValueDescription apply(Object e) {
        List<Method> methodList = Arrays.asList(e.getClass().getDeclaredMethods());

        String displayStr = null;
        String codeStr = null;
        String definitionStr = null;

        try {
            for (Method m : methodList) {
                if ("getDisplay".equals(m.getName())) {
                    displayStr = (String) m.invoke(e, null);
                } else if ("toCode".equals(m.getName())) {
                    codeStr = (String) m.invoke(e, null);
                } else if ("getDefinition".equals(m.getName())) {
                    definitionStr = (String) m.invoke(e, null);
                }
            }
            if (displayStr != null && codeStr != null && definitionStr != null) {
                return new AssertionFieldValueDescription(displayStr, codeStr, definitionStr);
            }
        } catch (Exception ex) {
        }
        return null;
    }
}
