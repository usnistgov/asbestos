package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;

import java.util.Arrays;
import java.util.List;

public class EventLinkToUILink {

    //  http://localhost:8081/asbestos/log/default/xds/Bundle/2020_02_26_17_15_02_417
    // to
    // http://localhost:8082/session/default/channel/xds/lognav/2020_02_26_17_15_02_417
    static public String get(String eventURL, String tail) {
        List<String> parts = Arrays.asList(eventURL.split("/"));
        String channel = null;
        String testSession = null;
        String eventId = null;
        for (int i=0; i<parts.size(); i++) {
            if (parts.get(i).equals("log")) {
                testSession = parts.get(i + 1);
                channel = parts.get(i + 2);
                eventId = parts.get(i + 4);

                return get(testSession, channel, eventId, tail);
            }
        }
        return null;
    }

    static public String get(String testSession, String channel, String eventId, String tail) {
        if (tail != null && tail.startsWith("/"))
            tail = tail.substring(1);
        String base = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_UI_HOME_PAGE);
        return base + "/" +
                "session/" +
                testSession + "/channel/" +
                channel + "/lognav/" +
                eventId
                + (tail.equals("") ? "" : "/")
                + tail;

    }

    static public String get(UIEvent event, String tail) {
        return get(event.getTestSession(), event.getChannelId(), event.getEventName(), tail);
    }

    static public String get(String eventURL) {
        return get(eventURL, "");
    }
}
