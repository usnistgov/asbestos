package gov.nist.asbestos.asbestosProxySupport.client;

import gov.nist.asbestos.asbestosProxySupport.resolver.Ref;

import java.net.URI;
import java.util.List;

class QueryBuilder {

    static URI buildUrl(Ref ref, Class<?> resourceType, List<String> params) {
        Ref x = ref.withResource(resourceType);
        String refstring = x.toString() + "?" + String.join(";", params);
        return EncodeUri.get(refstring);
    }
}
