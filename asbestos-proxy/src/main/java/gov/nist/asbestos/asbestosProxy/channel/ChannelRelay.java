package gov.nist.asbestos.asbestosProxy.channel;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// synchronization objects for Channels
public class ChannelRelay {
    private static Logger log = Logger.getLogger(ChannelRelay.class);
    private String channelId;
    private File eventDir = null;

    private static Map<String, ChannelRelay> objects = new HashMap<>();

    private static synchronized ChannelRelay get(String channelId) {
        if (objects.containsKey(channelId))
            return objects.get(channelId);
        ChannelRelay obj = new ChannelRelay();
        obj.channelId = channelId;
        objects.put(channelId, obj);
        return obj;
    }

    public static File waitForEvent(String channelId) {
        ChannelRelay repo = get(channelId);

        try {
            repo.eventDir = null;
            repo.wait(30*1000);
            return repo.eventDir;
        } catch (InterruptedException e) {
            log.info("ChannelRepo: " + channelId + " interrupted");
        } finally {
            repo.eventDir = null;
        }
        return null;
    }

    public static void postEvent(String channelId, File eventDir) {
        ChannelRelay repo = get(channelId);
        repo.eventDir = eventDir;
        repo.notify();
    }
}
