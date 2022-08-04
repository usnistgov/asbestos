package gov.nist.asbestos.mhd.transforms;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.ListResource;

import java.util.HashMap;
import java.util.Map;

public class MhdV4Common {
    /**
     * Use discriminator to find if SS or Folder
     */
    public static boolean isCodedListType(BaseResource resource, String code) {
        if (resource instanceof ListResource) {
            ListResource listResource = (ListResource)resource;
            Map<String, String> listTypeMap = new HashMap<>();
            listTypeMap.putAll(MhdV4.listTypeMap); // FIXME: code key must be unique
            listTypeMap.putAll(MhdV410.listTypeMap);
            String system = listTypeMap.get(code);
            if (listResource.getCode().hasCoding(system, code)) {
                /* Check if cardinality is [1..1] */
                if (listResource.getCode().getCoding().stream().filter(e -> system.equals(e.getSystem()) && code.equals(e.getCode())).count() == 1)  {
                    return true;
                }
            }
        }
        return false;
    }

}
