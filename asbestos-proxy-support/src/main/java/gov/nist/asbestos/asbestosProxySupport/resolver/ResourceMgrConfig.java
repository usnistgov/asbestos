package gov.nist.asbestos.asbestosProxySupport.resolver;

public class ResourceMgrConfig {
    private static final int INTERNAL = 1;
    private static final int OPEN   = 2;

    private int state;

    public ResourceMgrConfig() {
        this.state = OPEN;
    }

    public void internalOnly() {
        this.state = INTERNAL;
    }

    public boolean isOpen() {
        return state == OPEN;
    }

    public boolean isInternalOnly() {
        return state == INTERNAL;
    }
}
