package gov.nist.asbestos.asbestosProxy.events

import groovy.json.JsonSlurper

class EventStoreItemFactory {

    static List<EventStoreItem> parse(String json) {
        def slurper = new JsonSlurper()
        def data = slurper.parseText(json)

        List<EventStoreItem> items = []

        (0..<data.events.size()).each { int ii ->
            EventStoreItem item = new EventStoreItem()
            def events = data.events
            item.eventId = events[ii].eventId
            item.actor = events[ii].actor
            item.resource = events[ii].resource
            item.verb = events[ii].verb
            items << item
        }

        items
    }
}
