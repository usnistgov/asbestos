package gov.nist.asbestos.asbestosProxy.events;


import java.io.File;

public class EventStoreItem {
    String eventId;
    String actor;
    String resource;
    String verb;
    File file;

    public String asJson() {
        '''
{
  "eventId": "EVENTID",
  "actor": "ACTOR",
  "resource": "RESOURCE",
  "verb": "VERB"
}'''.replace('EVENTID', eventId)
                .replace('ACTOR', actor)
                .replace('RESOURCE', resource)
                .replace('VERB', verb)
    }
}
