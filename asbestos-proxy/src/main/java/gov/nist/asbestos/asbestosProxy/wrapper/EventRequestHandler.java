package gov.nist.asbestos.asbestosProxy.wrapper;


import gov.nist.asbestos.asbestosProxy.events.EventStoreSearch;

import java.util.List;
import java.util.Map;

class EventRequestHandler {

    static String eventRequest(SimStore simStore, List<String> uriParts, Map<String, List<String>> parameters) {
        int last = -1;
        if (uriParts.isEmpty()) {
            // asking for /Event  ??? - all events??? - must be some restricting parameters
            if (parameters.containsKey("_last")) {   //}   hasProperty('_last')) {
                List<String> lasts = parameters.get("_last");
                last = Integer.parseInt(lasts.get(0));
            }
        }

        EventStoreSearch search  = new EventStoreSearch(simStore.getExternalCache(), simStore.getChannelId());
        Map<String, EventStoreItem> items = search.loadAllEventsItems(); // key is eventId
        List<String> eventIds = items.keySet().sort();
        eventIds = eventIds.reverse();
        if (last > -1) {
            eventIds = eventIds.take(last);
        }
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        buf.append('{ "events":[\n');
        eventIds.each { String eventId ->
            if (!first) buf.append(',')
            first = false
            buf.append(items[eventId].asJson())
        }
        buf.append('\n]}');
        buf.toString();
    }

}
