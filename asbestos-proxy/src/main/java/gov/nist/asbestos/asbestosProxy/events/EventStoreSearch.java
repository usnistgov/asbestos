package gov.nist.asbestos.asbestosProxy.events;



import java.io.File;
import java.util.Collection;
import java.util.Map;


class EventStoreSearch {
    File externalCache
    SimStore simStore
    File simDir
    Map<String, EventStoreItem> eventItems = [:]  // eventId ->

    EventStoreSearch(File externalCache, SimId channelId) {
        this.externalCache = externalCache
        simStore = new SimStore(externalCache)
        simStore.channelId = channelId
        simDir = simStore.simDir
    }

    Map<String, EventStoreItem> loadAllEventsItems() {
        eventItems = [:]

        Collection<File> actorFiles = simDir.listFiles() as List<File>
        actorFiles = actorFiles.findAll { File file ->
            file.isDirectory() && !file.name.startsWith('.') && !file.name.startsWith('_')
        }
        actorFiles.each { File actorFile ->
            Collection<File> resourceFiles = actorFile.listFiles() as List<File>
            resourceFiles = resourceFiles.findAll { File resourceFile ->
                resourceFile.isDirectory() && !resourceFile.name.startsWith('.') && !resourceFile.name.startsWith('_')
            }
            resourceFiles.each { File resourceFile ->
                Collection<File> eventFiles = resourceFile.listFiles() as List<File>
                eventFiles = eventFiles.findAll { File eventFile ->
                    eventFile.isDirectory() && !eventFile.name.startsWith('.') && !eventFile.name.startsWith('_')
                }
                eventFiles.each { File eventFile ->
                    File requestHeaderFile = new File(new File(eventFile, 'request'), 'request_header.txt')
                    String firstLine
                    requestHeaderFile.withReader { firstLine = it.readLine() }
                    String verb = ''
                    if (firstLine) {
                        String[] parts = firstLine.split(' ', 2)
                        if (parts.size() > 1)
                            verb = parts[0]
                    }

                    EventStoreItem item = new EventStoreItem()
                    item.file = eventFile
                    item.eventId = eventFile.name
                    item.actor = actorFile.name
                    item.resource = resourceFile.name
                    item.verb = verb

                    eventItems[eventFile.name] = item
                }

            }
        }
        eventItems
    }
}
