package gov.nist.asbestos.mhd.resolver;
/**
 * used to configure ResourceMgr
 */
class ResolverConfig {
    boolean relativeReferenceOk = true;
    boolean relativeReferenceRequired = false;

    boolean externalRequired = false;
    boolean internalRequired = false;

    boolean containedOk = false;
    boolean containedRequired = false;

    ResolverConfig noRelative() {
        relativeReferenceOk = false;
        return this;
    }

    ResolverConfig relativeRequired() {
        relativeReferenceRequired = true;
        return this;
    }

    ResolverConfig externalRequired() {
        externalRequired = true;
        return this;
    }

    ResolverConfig internalRequired() {
        internalRequired = true;
        return this;
    }

    ResolverConfig containedOk() {
        containedOk = true;
        return this;
    }

    ResolverConfig containedRequired() {
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
