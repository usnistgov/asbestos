package gov.nist.asbestos.client.events;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Returns;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventListing {
    private static final int MAX_EVENT_LIMIT = 5000;
    private HttpServletResponse resp;
    private String testSession;
    private String channelId;
    private File externalCache;
    private File fhirDir;

    class ProxyChannelEventId {
        private String eventId;
        private String resourceType;

        public ProxyChannelEventId(String eventId, String resourceType) {
            this.eventId = eventId;
            this.resourceType = resourceType;
        }

        public String getEventId() {
            return eventId;
        }

        public String getResourceType() {
            return resourceType;
        }
    }

    public EventListing(HttpServletResponse resp, String testSession, String channelId, File externalCache) {
        this.resp = resp;
        this.testSession = testSession;
        this.channelId = channelId;
        this.externalCache = externalCache;
        this.fhirDir = new EC(externalCache).fhirDir(testSession, channelId);
    }

    public void buildJsonListingOfEvents(String resourceType) {
        File resourceTypeFile = new File(fhirDir, resourceType);

        List<String> events = Dirs.dirListingAsStringList(resourceTypeFile);
        returnJsonList(events);
    }

    public void returnJsonList(List<String> theList) {
        String json = new Gson().toJson(theList);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void buildJsonListingOfSingleEvent(String eventId) throws IOException {
        List<String> resourceTypes = Dirs.dirListingAsStringList(fhirDir);
        List<EventSummary> eventSummaries = new ArrayList<>();

        for (String resourceType : resourceTypes) {
            File resourceDir = new File(fhirDir, resourceType);
                File eventFile = new File(resourceDir, eventId);
                EventSummary summary = new EventSummary(eventFile);
                summary.resourceType = resourceType;
                summary.eventName = eventId;
                eventSummaries.add(summary);
        }
        String json = new Gson().toJson(eventSummaries);
        Returns.returnString(resp, json);
    }


    public void buildJsonListingOfEventSummaries(final List<String> toBeLoadedEventIds, boolean onlyReturnSpecifiedEvents) throws IOException {
        List<ProxyChannelEventId> eventIds = getAllEventsIds();
        if (! eventIds.isEmpty()) {
            eventIds = eventIds.stream().sorted(Comparator.comparing(ProxyChannelEventId::getEventId).reversed()).collect(Collectors.toList());

            Stream<ProxyChannelEventId> s = eventIds.stream();
            List<EventSummary> eventSummaries = loadEvents(s, toBeLoadedEventIds, onlyReturnSpecifiedEvents);
            String json = new Gson().toJson(eventSummaries);
            Returns.returnString(resp, json);
        } else {
            Returns.returnString(resp, "[]");
        }
    }

    private List<ProxyChannelEventId> getAllEventsIds() {
        List<String> resourceTypes = Dirs.dirListingAsStringList(fhirDir);
        List<ProxyChannelEventId> eventIds = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            File resourceDir = new File(fhirDir, resourceType);
            eventIds.addAll( Dirs.dirListingAsStringList(resourceDir, false).stream().map(e -> new ProxyChannelEventId(e, resourceType)).collect(Collectors.toList()));
        }
        return eventIds;
    }

    public void buildJsonListingOfEventSummaries(int itemsPerPage, int pageNum, int previousPageSize) throws IOException {
        List<ProxyChannelEventId> eventIds = getAllEventsIds();
        if (! eventIds.isEmpty()) {
            eventIds = eventIds.stream().sorted(Comparator.comparing(ProxyChannelEventId::getEventId).reversed()).collect(Collectors.toList());

            int totalEventCount = eventIds.size();
            Stream<ProxyChannelEventId> s = eventIds.stream().limit(MAX_EVENT_LIMIT);
            BigDecimal pageCount = (itemsPerPage > 0) ? new BigDecimal(String.valueOf(Math.ceil((double)eventIds.size() / (double)itemsPerPage))) : new BigDecimal(1);
            final int newPageNum = (pageCount.intValue() > 1) ? getNewPageNum(previousPageSize, pageNum, itemsPerPage, pageCount.intValue()) : 1;
            if (itemsPerPage > 0 && newPageNum > 0) {
                s = s.skip(itemsPerPage * (newPageNum - 1)).limit(itemsPerPage);
            }
            List<EventSummary> eventSummaries = loadEvents(s, null, false);
            if (eventSummaries != null && eventSummaries.size() > 0) {
                // add to only first item in the eventSummary index
                EventSummary summary = eventSummaries.get(0);
                summary.setTotalEventCount(totalEventCount);
                summary.setTotalPageableItems(pageCount.intValue());
                if (newPageNum != pageNum) {
                    summary.setNewPageNum(newPageNum);
                }
                String json = new Gson().toJson(eventSummaries);
                Returns.returnString(resp, json);
            }
        } else {
            Returns.returnString(resp, "[]");
        }
    }

    private List<EventSummary> loadEvents(Stream<ProxyChannelEventId> s, List<String> toBeLoadedEvents, boolean onlyReturnSpecifiedEvents) {
        Objects.requireNonNull(s);
        List<EventSummary> eventSummaries = new ArrayList<>();
        s.forEach(eventId -> {
            String resourceType = eventId.getResourceType();
            File resourceDir = new File(fhirDir, resourceType);
            File eventFile = new File(resourceDir, eventId.getEventId());
            if (/* preload all */ toBeLoadedEvents == null
                    || /* preload list */ (toBeLoadedEvents != null && toBeLoadedEvents.stream().anyMatch(e -> e.equals(eventId.getEventId())))) {
                EventSummary summary = new EventSummary();
                summary.resourceType = resourceType;
                summary.eventName = eventId.getEventId();
                // load only these request properties below
                // `${summary.verb} ${summary.resourceType} ${summary.ipAddr}`
                summary.loadResponseUiTask(eventFile);
                summary.loadRequestUiTask(eventFile);
                eventSummaries.add(summary);
            } else if (! onlyReturnSpecifiedEvents) {
                /* include lightweight wrapper of EventIds of non-matching items from toBeLoaded */
                EventSummary summary = new EventSummary();
                summary.resourceType = resourceType;
                summary.eventName = eventId.getEventId();
                eventSummaries.add(summary);
            }
        });
        return eventSummaries;
    }

    private int getNewPageNum(int previousPageSize, int previousPageNum, int currentItemsPerPage, int pageCount) {
        /**
         * Example
         * If PageSize was switched to 10 items per page from 25 items per page, current Page being 2, then,
         * This method computes the equivalent of the page that contains the first item in Page 2 (Index 26). Result should be Page 3, ItemsPerPage 10. This range contains 21-30 index items.
         */
        if (previousPageNum > 1 && /* previousPageNum < pageCount   && */ previousPageSize  > 0 && currentItemsPerPage != previousPageSize) {
            int newPageNum = 1;
            int currentPos = 0;
            while ((currentPos+previousPageSize) < previousPageSize * previousPageNum) {
                currentPos += previousPageSize;
            }

            while (currentPos+1 > newPageNum * currentItemsPerPage) {
                newPageNum++;
            }

            return (newPageNum < pageCount) ? newPageNum : 1;
        }

        return (previousPageNum > 0 && previousPageNum  <= pageCount)? previousPageNum : 1;
    }

    public void buildJsonListingOfResourceTypes() {
        List<String> resourceTypes = Dirs.dirListingAsStringList(fhirDir);
        returnJsonList(resourceTypes);
    }

}
