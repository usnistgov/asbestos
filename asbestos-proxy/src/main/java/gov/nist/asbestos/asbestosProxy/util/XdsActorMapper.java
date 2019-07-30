package gov.nist.asbestos.asbestosProxy.util;

import java.util.HashMap;
import java.util.Map;

public class XdsActorMapper {
    private static Map<String, String> transToEpt = new HashMap<>();
    static {
        transToEpt.put("pnr", "prb");
        transToEpt.put( "r", "r");
    }

    public String getEndpoint(String siteName, String actorType, String transactionType, boolean isTls) {
        return
                (isTls ? "https" : "http") +
                "://localhost:8080/xdstools/sim/" +
                siteName + "/" +
                actorType + "/" +
                        transToEpt.get(transactionType);

        //return "http://localhost:8080/xdstools/sim/default__rr/rep/prb";
    }
}
