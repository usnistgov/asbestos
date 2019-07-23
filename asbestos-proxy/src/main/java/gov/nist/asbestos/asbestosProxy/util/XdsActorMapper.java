package gov.nist.asbestos.asbestosProxy.util;

public class XdsActorMapper {

    public String getEndpoint(String siteName, String actorType, String transactionType, boolean isTls) {
        return "http://localhost:8080/xdstools/sim/default__rr/rep/prb";
    }
}
