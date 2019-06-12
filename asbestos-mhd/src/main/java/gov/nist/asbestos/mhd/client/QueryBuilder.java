package gov.nist.asbestos.mhd.client;

import gov.nist.asbestos.mhd.resolver.Ref;

import java.net.URI;
import java.util.List;

public class QueryBuilder {

    public static URI buildUrl(Ref ref, Class<?> resourceType, List<String> params) {
        Ref x = ref.withResource(resourceType);
        String refstring = x.toString() + "?" + String.join(";", params);
        return EncodeUri.get(refstring);
    }
}
