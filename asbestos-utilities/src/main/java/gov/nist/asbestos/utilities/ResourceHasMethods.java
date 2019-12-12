package gov.nist.asbestos.utilities;

import com.google.gson.Gson;
import org.hl7.fhir.r4.model.Resource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.nist.asbestos.client.Base.ProxyBase.getFhirContext;

public class ResourceHasMethods {

    private static class CaseInsensitiveString {
        private String string;

        public CaseInsensitiveString(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CaseInsensitiveString that = (CaseInsensitiveString) o;

            return string.equalsIgnoreCase(that.string);
        }

        @Override
        public int hashCode() {
            return string.toLowerCase().hashCode();
        }
    }


    public static <T extends Resource> String toJson(T resourceObj) {

        Objects.requireNonNull(resourceObj);

        List<Method> methodList = new ArrayList<>();

        // Add super class methods to catch methods like hasId
        Class parent = resourceObj.getClass().getSuperclass();
        while (parent != null) {
            methodList.addAll(Arrays.asList(parent.getDeclaredMethods()));
            parent = parent.getSuperclass();
        }

        methodList.addAll(Arrays.asList(resourceObj.getClass().getDeclaredMethods()));

        // For each resourceObj method name starting with "has", if resourceObj.has(M) is True, collect M into a Set
         Set<CaseInsensitiveString> hasMethodSet = methodList.stream()
                 .filter(m -> m.getName().startsWith("has"))
                 .filter((m) -> {
                     try {
                        return (boolean)m.invoke(resourceObj, null);
                        } catch (Throwable t) {}
                            return false;
                    })
                 .map(Method::getName)
                 .map(s -> new CaseInsensitiveString(s.substring(3)))
                 .collect(Collectors.toSet());

        String jsonFullResource = getFhirContext().newJsonParser().setPrettyPrint(false).encodeResourceToString(resourceObj);
        Map<String, List<String>> myMap = new Gson().fromJson(jsonFullResource, Map.class);

       // Store keys as insensitive case
       // This is to address the case where methods hasStatus and hasStatusElement both refer to the same contextual base resource element but we should only pick the keys that exist in the output from the Fhir-JSON parser
       List<CaseInsensitiveString> ciStringList = myMap.entrySet().stream()
               .map(e -> new CaseInsensitiveString(e.getKey()))
               .collect(Collectors.toList());

        // Remove keys from the map that don't exist in hasMethodSet
       for (CaseInsensitiveString ciString : ciStringList) {
           if (! hasMethodSet.contains(ciString)) {
              myMap.remove(ciString.string);
           }
       }

       // Return map as JSON
        String json = new Gson().toJson(myMap);
        return json;
    }
}

