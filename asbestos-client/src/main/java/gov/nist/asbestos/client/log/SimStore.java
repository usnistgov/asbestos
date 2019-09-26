package gov.nist.asbestos.client.log;


import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.Task;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

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
                        throw new RuntimeException("SimStore: cannot create writable psimdb directory at " + _simStoreLocation);
            } else {
                if (!(_simStoreLocation.exists() && _simStoreLocation.canWrite() && _simStoreLocation.isDirectory()))
                    throw new RuntimeException("SimStore: Channel " + channelId.toString() + " does not exist");
            }
        }
        return  _simStoreLocation;
    }

    // format for each id is testsession__id
    public List<String> getChannelIds() {
        List<String> results = new ArrayList<>();
        File psimdb = new File(externalCache, PSIMDB);
        if (psimdb.isDirectory() && psimdb.canRead()) {
            File[] psimdbFiles = psimdb.listFiles();
            if (psimdbFiles != null) {
                for (File testSession : psimdbFiles) {
                    if (testSession.getName().startsWith("."))
                        continue;
                    if (testSession.toString().startsWith("_"))
                        continue;
                    File[] idFiles = testSession.listFiles();
                    if (idFiles != null) {
                        for (File id : idFiles) {
                            if (id.getName().startsWith("."))
                                continue;
                            if (id.getName().startsWith("_"))
                                continue;
                            results.add(testSession.getName() + "__" + id.getName());
                        }
                    }
                }
            }
        }
        return results;
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
        if (!existsChannelDir())
            throw new RuntimeException("Channel does not exist");
        File file = new File(getChannelDir(), CHANNEL_CONFIG_FILE);
        channelConfig = ChannelConfigFactory.load(file);
        channelId = getSimId(channelConfig);
        channelId.validate();
        return this;
    }

    public boolean exists() {
        Objects.requireNonNull(externalCache);
        Objects.requireNonNull(channelId);
        File f = new File(getStore(), channelId.getId());
        return f.exists();
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
            File dir = getChannelDir();
            FileUtils.deleteDirectory(dir);
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
        if (channelId.getActorType() == null)
            return "fhir";
        return channelId.getActorType();
    }

    public void setActor(String actor) {
        channelId.setActorType(actor);
    }

    public boolean existsChannelDir() {
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

    public List<File> getResourceTypeDirs() {
        File resourcesDir = new File(getChannelDir(), getActorType());
        return Dirs.listOfDirectories(resourcesDir);
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
        return new Event(createEventDir());
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

    private static String getEventIdFromDate(Date date) {
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

}
