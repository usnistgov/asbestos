package gov.nist.asbestos.client.events;



import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.simapi.simCommon.SimId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;


public class EventStoreSearch {
    private File simDir;

    public EventStoreSearch(File externalCache, SimId channelId) {
        SimStore simStore = new SimStore(externalCache);
        simStore.setChannelId(channelId);
        simDir = simStore.getChannelDir();
    }

    private boolean focus(File x) {
        return x.isDirectory() && !x.getName().startsWith(".") && !x.getName().startsWith("_");
    }

    public EventStoreItem getMostRecent() {
        Map<String, EventStoreItem> eventItems = loadAllEventsItems();
        Optional<String> theItem = eventItems.keySet().stream().max(Comparator.naturalOrder());
        return theItem.map(eventItems::get).orElse(null);
    }

    public Map<String, EventStoreItem> loadAllEventsItems() {
        Map<String, EventStoreItem> eventItems = new HashMap<>();

        File[] actorFileA = simDir.listFiles();
        if (actorFileA != null) {
            List<File> actorFiles = Arrays.stream(actorFileA)
                    .filter(this::focus)
                    .collect(Collectors.toList());
            for (File actorFile : actorFiles) {
                File[] resourceFileA = actorFile.listFiles();
                if (resourceFileA != null) {
                    List<File> resourceFiles = Arrays.stream(resourceFileA)
                            .filter(this::focus)
                            .collect(Collectors.toList());
                    for (File resourceFile : resourceFiles) {
                        File[] eventFileA = resourceFile.listFiles();
                        if (eventFileA != null) {
                            List<File> eventFiles = Arrays.stream(eventFileA)
                                    .filter(this::focus)
                                    .collect(Collectors.toList());
                            for (File eventFile : eventFiles) {
                                File requestHeaderFile = new File(new File(eventFile, "request"), "request_header.txt");
                                String firstLine = null;
                                try {
                                    firstLine = new BufferedReader(new FileReader(requestHeaderFile)).readLine();
                                } catch (Exception e) {

                                }
                                String verb = "";
                                if (firstLine != null) {
                                    String[] parts = firstLine.split(" ", 2);
                                    if (parts.length > 1)
                                        verb = parts[0];
                                }

                                EventStoreItem item = new EventStoreItem();
                                item.file = eventFile;
                                item.eventId = eventFile.getName();
                                item.actor = actorFile.getName();
                                item.resource = resourceFile.getName();
                                item.verb = verb;

                                eventItems.put(eventFile.getName(), item);
                            }
                        }
                    }
                }
            }
        }
        return eventItems;
    }
}
