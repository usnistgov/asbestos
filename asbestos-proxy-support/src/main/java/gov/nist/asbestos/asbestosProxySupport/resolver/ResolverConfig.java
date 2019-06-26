package gov.nist.asbestos.asbestosProxySupport.resolver;

/**
 * used to configure ResourceMgr
 */
public class ResolverConfig {
    private boolean relativeReferenceOk = true;

    private boolean externalRequired = false;
    private boolean inBundleRequired = false;

    private boolean containedOk = false;
    private boolean containedRequired = false;

    public ResolverConfig noRelative() {
        relativeReferenceOk = false;
        return this;
    }

    public ResolverConfig relativeOk() {
        this.relativeReferenceOk = true;
        return this;
    }

    public ResolverConfig externalRequired() {
        externalRequired = true;
        return this;
    }

    public ResolverConfig inBundleRequired() {
        inBundleRequired = true;
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

    public boolean isInBundleOk() {
        return !externalRequired;
    }

    public boolean isContainedOk() {
        return containedOk;
    }

    boolean isRelativeOk() {
        return relativeReferenceOk;
    }

    public String toString() {
        return ((!relativeReferenceOk) ? " relativeNotAllowed" : "") +
                ((externalRequired) ? " externalRequired" : "") +
                ((inBundleRequired) ? " inBundleRequired" : "") +
                ((containedOk) ? " containedOk" : "") +
                ((containedRequired) ? " containedRequired" : "");
    }

}
