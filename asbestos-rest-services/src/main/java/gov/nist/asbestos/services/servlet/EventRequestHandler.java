package gov.nist.asbestos.services.servlet;


import gov.nist.asbestos.client.events.EventStoreItem;
import gov.nist.asbestos.client.events.EventStoreItemFactory;
import gov.nist.asbestos.client.events.EventStoreSearch;
import gov.nist.asbestos.client.log.SimStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EventRequestHandler {

    public static String eventRequest(SimStore simStore, List<String> uriParts, Map<String, List<String>> parameters) {
        int last = -1;
        if (uriParts.isEmpty()) {
            // asking for /TaskStore  ??? - all events??? - must be some restricting parameters
            if (parameters.containsKey("_last")) {   //}   hasProperty('_last')) {
                List<String> lasts = parameters.get("_last");
                last = Integer.parseInt(lasts.get(0));
            }
        }

        EventStoreSearch search  = new EventStoreSearch(simStore.getExternalCache(), simStore.getChannelId());
        Map<String, EventStoreItem> items = search.loadAllEventsItems(); // key is eventId
        List<String> eventIds = new ArrayList<>(items.keySet());
        eventIds.sort(String::compareTo);
        Collections.reverse(eventIds);
        if (last > -1) {
            eventIds = eventIds.subList(0, last);
        }
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        buf.append("{ \"events\":[\n");
        for (String eventId : eventIds) {
            if (!first) buf.append(",");
            first = false;
            buf.append(EventStoreItemFactory.asJson(items.get(eventId)));
        }
        buf.append("\n]}");
        return buf.toString();
    }

}
