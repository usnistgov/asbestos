package gov.nist.asbestos.client.client;

import gov.nist.asbestos.client.resolver.Ref;

import java.net.URI;
import java.util.List;

class QueryBuilder {

    static URI buildUrl(Ref ref, Class<?> resourceType, List<String> params) {
        Ref x = ref.withResource(resourceType.getSimpleName());
        String refstring = x.toString() + "?" + String.join("&", params);
        return EncodeUri.get(refstring);
    }
}
