package gov.nist.asbestos.client.log;


import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.Task;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Date;
import java.util.Objects;

import static java.lang.Thread.sleep;

/**
 * Store is organized as:
 * EC/testSession/psimdb/channelId/actor/resource/taskStore/event_files
 * The content starting from the taskStore/ is handed off to the class Event
 */
public class SimStore {
    private File externalCache;
    private File _simStoreLocation = null;
    private File _channelIdDir = null;
    private File _resourceDir = null;
    private File _actorDir;
    private File _eventDir = null;

    private SimId channelId;
    private String resource = null;
    private String eventId = null; // within resource

    private boolean newlyCreated = false;
    private static final String PSIMDB = "psimdb";
    private static final String CHANNEL_CONFIG_FILE = "config.json";
    private Task task;
    //Event eventStore
    ChannelConfig channelConfig;
    private boolean channel = true;  // is this a channel to the backend system?

    public SimStore(File externalCache, SimId channelId) {
        Installation.validateExternalCache(externalCache);
        this.externalCache = externalCache;
        this.channelId = channelId;
    }

    public SimStore(File externalCache) {
        Installation.validateExternalCache(externalCache);
        this.externalCache = externalCache;
    }

    // the following must initialized
    // externalCache
    // channelId
    File getStore(boolean create)  {
        if (_simStoreLocation == null) {
            _simStoreLocation = testSessionDir(externalCache, channelId);
            if (create) {
                newlyCreated = !_simStoreLocation.exists();
                // assert !_simStoreLocation.exists() : "SimStore:Create: proxy ${channelId} at ${_simStoreLocation} already exists\n"
                _simStoreLocation.mkdirs();
                if(!(_simStoreLocation.exists() && _simStoreLocation.canWrite() && _simStoreLocation.isDirectory()))
                        throw new RuntimeException("SimStore: cannot create writable simdb directory at " + _simStoreLocation);
            } else {
                if (!(_simStoreLocation.exists() && _simStoreLocation.canWrite() && _simStoreLocation.isDirectory()))
                    throw new RuntimeException("SimStore: Sim " + channelId.toString() + " does not exist");
            }
        }
        return  _simStoreLocation;
    }

    public SimStore create(ChannelConfig channelConfig) {
        this.channelConfig = channelConfig;
        getStore(true);
        channelId = getSimId(channelConfig);
        if (!exists())
            newlyCreated = true;
        channelId.validate();
        ChannelConfigFactory.store(channelConfig, new File(getChannelDir(), CHANNEL_CONFIG_FILE));
        return this;
    }

    public SimStore open() {
        getStore(false);
        channelConfig = ChannelConfigFactory.load(new File(getChannelDir(), CHANNEL_CONFIG_FILE));
        channelId = getSimId(channelConfig);
        channelId.validate();
        return this;
    }

    public boolean exists() {
        Objects.requireNonNull(externalCache);
        Objects.requireNonNull(channelId);
        return new File(getStore(), channelId.getId()).exists();
    }

    public static SimId getSimId(ChannelConfig channelConfig) {
        return new SimId(new TestSession(channelConfig.getTestSession()), channelConfig.getChannelId(), channelConfig.getActorType(), channelConfig.getEnvironment());
    }

    public File getStore() {
        return getStore(false);
    }

    public boolean expectingEvent() {
        return channelId != null && channelId.getActorType() != null && resource != null;
    }

    public void deleteSim() {
        try {
            FileUtils.deleteDirectory(getChannelDir());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setChannelId(SimId simId) {
        if (simId.validateState() != null)
            throw new RuntimeException("SimStore: cannot open SimId " + simId + ":\n" + simId.validateState());
        this.channelId = simId;
    }

    public void setSimIdForLoader(SimId simId) {
        this.channelId = simId;
    }

    public File testSessionDir(File externalCache, SimId simId) {
        return new File(new File(externalCache, PSIMDB), simId.getTestSession().getValue());
    }

    public String getActorType() {
        return channelId.getActorType();
    }

    public void setActor(String actor) {
        channelId.setActorType(actor);
    }

    public boolean existsSimDir() {
        Objects.requireNonNull(channelId);
        if (_channelIdDir == null)
            _channelIdDir = new File(getStore(), channelId.getId());
        return _channelIdDir.exists();
    }

    public File getChannelDir() {
        Objects.requireNonNull(channelId);
        if (_channelIdDir == null)
            _channelIdDir = new File(getStore(), channelId.getId());
        _channelIdDir.mkdirs();
        return _channelIdDir;
    }

    public File getActorDir() {
        Objects.requireNonNull(getActorType());
        if (_actorDir == null)
            _actorDir = new File(getChannelDir(), getActorType());
        _actorDir.mkdirs();
        return _actorDir;
    }


    public File getResourceDir() {
        Objects.requireNonNull(resource);
        if (_resourceDir == null)
            _resourceDir = new File(getActorDir(), resource);
        _resourceDir.mkdirs();
        return _resourceDir;
    }

    private File getEventDir(String eventId) {
        Objects.requireNonNull(eventId);
        File eventDir;
        if (_eventDir == null)
            _eventDir = new File(getResourceDir(), eventId);
       // _eventDir.mkdirs()  // breaks createEventDir(date)
        return _eventDir;
    }

    public Event newEvent() {
        return new Event(this, createEventDir());
    }

    // on some machines this is important to prevent hangs
    private static void pause()  {
        try {
            sleep(5);
        } catch (InterruptedException e) {

        }
    }

    private File createEventDir() {
        return createEventDir(new Date());
    }

    private File createEventDir(Date date)  {
        File f = createEventDir(getEventIdFromDate(date));
        f.mkdirs();
        pause();
        return f;
    }

    private String getEventIdFromDate(Date date) {
        return Installation.asFilenameBase(date);
    }

    /**
     * Given base name of a new taskStore - extend it to ensure it is unique.
     * Does not create the directory, just the File
     * @param eventBase - usually date/time stamp
     * @return
     */
    private File createEventDir(String eventBase) {
        int incr = 0;
        File eventDir;  // restart
        while (true) {
            eventId = eventBase;
            if (incr != 0)
                eventId = eventBase + '_' + incr;    // make unique
            eventDir = new File(getResourceDir(), eventId);
            if (eventDir.exists()) {
                // must be fresh new dir - try again
                incr++;
            }
            else
                break;
        }
        return eventDir;
    }

    public String getEndpoint() {
//        if (!channelConfig.fhirBase.endsWith('/'))
//            channelConfig.fhirBase = "${channelConfig.fhirBase}/"
        return channelConfig.getFhirBase() + "/" + resource;
    }

    public SimId getChannelId() {
        return channelId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getEventId() {
        return eventId;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    public File getExternalCache() {
        return externalCache;
    }

    public boolean isChannel() {
        return channel;
    }

    public void setChannel(boolean channel) {
        this.channel = channel;
    }

    public ChannelConfig getChannelConfig() {
        return channelConfig;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    //    public static String asFilenameBase(Date date) {
//        Calendar c  = Calendar.getInstance();
//        c.setTime(date);
//
//        String year = Integer.toString(c.get(Calendar.YEAR));
//        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
//        if (month.length() == 1)
//            month = "0" + month;
//        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
//        if (day.length() == 1 )
//            day = "0" + day;
//        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
//        if (hour.length() == 1)
//            hour = "0" + hour;
//        String minute = Integer.toString(c.get(Calendar.MINUTE));
//        if (minute.length() == 1)
//            minute = "0" + minute;
//        String second = Integer.toString(c.get(Calendar.SECOND));
//        if (second.length() == 1)
//            second = "0" + second;
//        String mili = Integer.toString(c.get(Calendar.MILLISECOND));
//        if (mili.length() == 2)
//            mili = "0" + mili;
//        else if (mili.length() == 1)
//            mili = "00" + mili;
//
//        String dot = "_";
//
//        String val =
//                year +
//                        dot +
//                        month +
//                        dot +
//                        day +
//                        dot +
//                        hour +
//                        dot +
//                        minute +
//                        dot +
//                        second +
//                        dot +
//                        mili
//        ;
//        return val;
//    }
}
