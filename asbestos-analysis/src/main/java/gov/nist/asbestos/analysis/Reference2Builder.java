package gov.nist.asbestos.analysis;

import gov.nist.asbestos.utilities.ResourceHasMethodsFilter;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Build map of references from one Resource to others
class Reference2Builder {

    static List<Reference2> buildReferences(BaseResource resource) {
        Map atts = ResourceHasMethodsFilter.toMap(resource);
        List<Reference2> refs = new ArrayList<>();
        buildReferences2(atts, refs, "????");
        return refs;
    }

    private static void buildReferences2(Map atts, List<Reference2> refs, String lastKey) {
        for (Object okey : atts.keySet()) {
            String key = (String) okey;
            Object value = atts.get(okey);
            if ("reference".equals(key) && value instanceof String) {
                refs.add(new Reference2(lastKey, (String) value));
            } else if ("url".equals(key) && value instanceof String) {
                refs.add(new Reference2(lastKey, (String) value));
            } else if (value instanceof Map) {
                buildReferences2((Map)value, refs, key);
            } else if (value instanceof List) {
                for (Object o : (List) value) {
                    if (o instanceof Map) {
                        buildReferences2((Map) o, refs, key);
                    }
                }
            }
        }
    }
}
