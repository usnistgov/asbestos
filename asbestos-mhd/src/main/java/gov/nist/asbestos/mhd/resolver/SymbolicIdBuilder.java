package gov.nist.asbestos.mhd.resolver;

public class SymbolicIdBuilder {

    private int symbolicIdCounter = 1;

    public String allocate() {
        return "ID" + symbolicIdCounter++;
    }

}
