package gov.nist.asbestos.asbestosProxy.log;


import gov.nist.asbestos.asbestosProxy.channel.ChannelConfig;
import gov.nist.asbestos.asbestosProxy.events.Event;
import gov.nist.asbestos.asbestosProxy.events.EventStore;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static java.lang.Thread.sleep;

/**
 * Store is organized as:
 * EC/testSession/psimdb/channelId/actor/resource/event/event_files
 * The content starting from the event/ is handed off to the class EventStore
 */
public class SimStore {
    private File externalCache;
    private File _simStoreLocation = null;
    private File _simIdDir = null;
    private File _resourceDir = null;
    private File _actorDir;
    private File _eventDir = null;

    private SimId channelId;
    private String resource = null;
    private String eventId = null; // within resource

    private boolean newlyCreated = false;
    private static String PSIMDB = "psimdb";
    Event event;
    //EventStore eventStore
    ChannelConfig config;
    boolean channel = true;  // is this a channel to the backend system?

    public SimStore(File externalCache, SimId channelId) throws Exception {
        if (externalCache == null)
            throw new Exception("SimStore: initialized with externalCache == null");
        if (!channelId.validateState())
            throw new Exception("SimStore: cannot open SimId " + channelId + ":\n" + channelId.validateState());
        this.externalCache = externalCache;
        this.channelId = channelId;
    }

    public SimStore(File externalCache) throws Exception {
        if (externalCache == null)
            throw new Exception("SimStore: initialized with externalCache == null");
        this.externalCache = externalCache;
    }

    // the following must initialized
    // externalCache
    // channelId
    public File getStore(boolean create) throws Exception {
        if (!externalCache.exists())
                throw new Exception("SimStore: External Cache must exist (" + externalCache + ")");
        if (_simStoreLocation == null) {
            _simStoreLocation = testSessionDir(externalCache, channelId);
            if (create) {
                newlyCreated = !_simStoreLocation.exists();
                // assert !_simStoreLocation.exists() : "SimStore:Create: proxy ${channelId} at ${_simStoreLocation} already exists\n"
                _simStoreLocation.mkdirs();
                if(!(_simStoreLocation.exists() && _simStoreLocation.canWrite() && _simStoreLocation.isDirectory())
                        throw new Exception("SimStore: cannot create writable simdb directory at " + _simStoreLocation);
            } else {
                if (!(_simStoreLocation.exists() && _simStoreLocation.canWrite() && _simStoreLocation.isDirectory())
                    throw new Exception("SimStore: Sim " + channelId.toString() + " does not exist");
            }
        }
        if (channelId.actorType == null && config != null)
            channelId.actorType = config.actorType;
        return  _simStoreLocation;
    }

    public boolean exists() throws Exception {
        if (!externalCache.exists())
            throw new Exception("SimStore: External Cache must exist: " + externalCache);
        return new File(getStore(), channelId.getId()).exists();
    }

    public File getStore() {
        return getStore(false);
    }

    public boolean expectingEvent() {
        return channelId && channelId.actorType && resource;
    }

    public boolean deleteSim() {
        return simDir.deleteDir();
    }

    public void setChannelId(SimId simId) throws Exception {
        if (!simId.validateState())
            throw new Exception("SimStore: cannot open SimId " + simId + ":\n" + simId.validateState());
        this.channelId = simId;
    }

    public void setSimIdForLoader(SimId simId) {
        this.channelId = simId;
    }

    public File testSessionDir(File externalCache, SimId simId) {
        return new File(new File(externalCache, PSIMDB), simId.getTestSession().getValue());
    }

    public String getActor() {
        return return channelId.getActorType();
    }

    public void setActor(String actor) {
        channelId.actorType = actor;
    }

    public boolean existsSimDir() throws Exception {
        if (channelId == null)
            throw new Exception("SimStore: channelId is null");
        if (_simIdDir == null)
            _simIdDir = new File(getStore(), channelId.getId());
        return _simIdDir.exists();
    }

    public File getSimDir() throws Exception {
        if (channelId == null)
            throw new Exception("SimStore: channelId is null");
        if (_simIdDir == null)
            _simIdDir = new File(getStore(), channelId.getId());
        _simIdDir.mkdirs();
        return _simIdDir;
    }

    public File getActorDir() throws Exception {
        Objects.requireNonNull(actor);
        if (_actorDir == null)
            _actorDir = new File(simDir, actor);
        _actorDir.mkdirs();
        return _actorDir;
    }


    public File getResourceDir() {
        Objects.requireNonNull(resource);
        if (_resourceDir == null)
            _resourceDir = new File(actorDir, resource);
        _resourceDir.mkdirs();
        return _resourceDir;
    }

    public File getEventDir() {
        Objects.requireNonNull(eventId);
        if (_eventDir == null)
            _eventDir = new File(resourceDir, eventId);
       // _eventDir.mkdirs()  // breaks createEvent(date)
        return _eventDir;
    }

    public Event newEvent() {
        createEvent();
        EventStore eventStore = new EventStore(this, _eventDir);
        event = eventStore.newEvent();
        return event;
    }

    // on some machines this is important to prevent hangs
    private static void pause() throws InterruptedException {
        sleep(5);
    }

    public File createEvent() {
        return createEvent(new Date());
    }

    public File createEvent(Date date) throws InterruptedException {
        File f = createEventDir(getEventIdFromDate(date));
        f.mkdirs();
        pause();
        return f;
    }

    public File useEvent(String eventId) {
        return createEventDir(eventId);
    }

    public SimStore withResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SimStore withActorType(String actor) {
        this.channelId.actorType = actor;
        return this;
    }

    public String getEventIdFromDate(Date date) {
        return asFilenameBase(date);
    }

    /**
     * Given base name of a new event - extend it to ensure it is unique.
     * Does not create the directory, just the File
     * @param eventBase - usually date/time stamp
     * @return
     */
    private File createEventDir(String eventBase) {
        int incr = 0;
        _eventDir = null;  // restart
        while (true) {
            eventId = eventBase;
            if (incr != 0)
                eventId = eventBase + '_' + incr;    // make unique
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
//        if (!config.fhirBase.endsWith('/'))
//            config.fhirBase = "${config.fhirBase}/"
        return config.fhirBase + "/" + resource;
    }


    public static String asFilenameBase(Date date) {
        Calendar c  = Calendar.getInstance();
        c.setTime(date);

        String year = Integer.toString(c.get(Calendar.YEAR));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        if (month.length() == 1)
            month = "0" + month;
        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1 )
            day = "0" + day;
        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1)
            hour = "0" + hour;
        String minute = Integer.toString(c.get(Calendar.MINUTE));
        if (minute.length() == 1)
            minute = "0" + minute;
        String second = Integer.toString(c.get(Calendar.SECOND));
        if (second.length() == 1)
            second = "0" + second;
        String mili = Integer.toString(c.get(Calendar.MILLISECOND));
        if (mili.length() == 2)
            mili = "0" + mili;
        else if (mili.length() == 1)
            mili = "00" + mili;

        String dot = "_";

        String val =
                year +
                        dot +
                        month +
                        dot +
                        day +
                        dot +
                        hour +
                        dot +
                        minute +
                        dot +
                        second +
                        dot +
                        mili
        ;
        return val;
    }
}
