package gov.nist.asbestos.asbestosProxySupport.resolver;

public class SymbolicIdBuilder {

    private int symbolicIdCounter = 1;

    public String allocate() {
        return "ID" + symbolicIdCounter++;
    }

}
