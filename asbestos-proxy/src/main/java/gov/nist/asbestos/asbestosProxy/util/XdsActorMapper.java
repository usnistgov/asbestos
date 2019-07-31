package gov.nist.asbestos.asbestosProxy.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class XdsActorMapper {

    public URI getEndpoint(String siteName, String actorType, String transactionType, boolean isTls) {
        try {
            return new URI(
                    (isTls ? "https" : "http") +
                    "://localhost:8080/xdstools/sim/" +
                    siteName + "/" +
                    actorType + "/" +
                            transactionType);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
