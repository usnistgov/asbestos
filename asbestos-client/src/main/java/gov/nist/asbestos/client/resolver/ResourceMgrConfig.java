package gov.nist.asbestos.client.resolver;

public class ResourceMgrConfig {
    enum State { INTERNAL, OPEN }

    private State state;

    public ResourceMgrConfig() {
        this.state = State.OPEN;
    }

    public void internalOnly() {
        this.state = State.INTERNAL;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    public boolean isInternalOnly() {
        return state == State.INTERNAL;
    }
}
