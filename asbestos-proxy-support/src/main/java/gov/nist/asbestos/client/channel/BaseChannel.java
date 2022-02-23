package gov.nist.asbestos.client.channel;

import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.client.Format;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseChannel implements IBaseChannel {
    protected ChannelConfig channelConfig = null;
    protected Format returnFormatType = null;
    protected ITask task = null;
    private String hostport = null;

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }

    public Format getReturnFormatType() {
        return returnFormatType;
    }

    public ITask getTask() {
        return task;
    }

    public void setTask(ITask task) {
        this.task = task;
    }

    public String getHostport() {
        return hostport;
    }

    public void setHostport(String hostport) {
        this.hostport = hostport;
    }

    public String getChannelId() {
        return channelConfig.asChannelId();
    }

    @Override
    public void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig;
    }

    public static String logReference(Logger log, String methodName, Throwable t) {
        String error = String.format("An %s error occurred at %s. Check server log based on the time this error occurred.\nMessage: %s\nException: %s", methodName, new Date().toString(), t.getMessage(), t.toString());
        log.log(Level.SEVERE, error, t);
        return error;
    }

}
