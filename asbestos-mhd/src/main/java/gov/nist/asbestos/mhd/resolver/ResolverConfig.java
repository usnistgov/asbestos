package gov.nist.asbestos.mhd.resolver;
/**
 * used to configure ResourceMgr
 */
public class ResolverConfig {
    boolean relativeReferenceOk = true;
    boolean relativeReferenceRequired = false;

    boolean externalRequired = false;
    boolean internalRequired = false;

    boolean containedOk = false;
    boolean containedRequired = false;

    public ResolverConfig noRelative() {
        relativeReferenceOk = false;
        return this;
    }

    public ResolverConfig relativeRequired() {
        relativeReferenceRequired = true;
        return this;
    }

    public ResolverConfig externalRequired() {
        externalRequired = true;
        return this;
    }

    public ResolverConfig internalRequired() {
        internalRequired = true;
        return this;
    }

    public ResolverConfig containedOk() {
        containedOk = true;
        return this;
    }

    public ResolverConfig containedRequired() {
        containedRequired = true;
        containedOk = true;
        return this;
    }

    public String toString() {
        return ((!relativeReferenceOk) ? " relativeNotAllowed" : "") +
                ((relativeReferenceRequired) ? " relativeRequired" : "") +
                ((externalRequired) ? " externalRequired" : "") +
                ((internalRequired) ? " internalRequired" : "") +
                ((containedOk) ? " containedOk" : "") +
                ((containedRequired) ? " containedRequired" : "");
    }

}
