package gov.nist.asbestos.http.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParameterBuilder {
    Map<String, List<String>> parameterMap = new HashMap<>();

    ParameterBuilder add(String name, String value) {
        List<String> values = parameterMap.get(name);
        if (values != null) {
            values.add(value);
        } else {
            values = new ArrayList<>();
            values.add(value);
            parameterMap.put(name, values);
        }

        return this;
    }
}
