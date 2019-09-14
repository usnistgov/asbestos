package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.client.events.Task;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.sharedObjects.ChannelConfig;

public abstract class BaseChannel implements IBaseChannel {
    protected ChannelConfig channelConfig = null;
    protected Format returnFormatType = null;
    protected Task task = null;
    private String hostport = null;

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }

    public Format getReturnFormatType() {
        return returnFormatType;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getHostport() {
        return hostport;
    }

    public void setHostport(String hostport) {
        this.hostport = hostport;
    }

    @Override
    public void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig;
    }
}
