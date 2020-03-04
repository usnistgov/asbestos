package gov.nist.asbestos.client.events;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.tk.installation.Installation;

import java.net.URI;

// Example: x-proxy-event:http://localhost:8081/asbestos/log/default/default/DocumentReference/2020_02_08_06_33_44_696
public class ProxyEvent {
    HttpBase httpBase;

    public ProxyEvent(HttpBase httpBase) {
        this.httpBase = httpBase;
    }

    public UIEvent getEvent() {
        Header proxyEventHeader = httpBase.getResponseHeaders().get("x-proxy-event");
        if (proxyEventHeader == null) return null;
        String proxyEventUrl = proxyEventHeader.getValue();
        if (proxyEventUrl == null || proxyEventUrl.equals("")) return null;
        URI proxyEventURI;
        try {
            proxyEventURI = new URI(proxyEventUrl);
        } catch (Exception e) {
            return null;
        }

        return new UIEvent(new EC(Installation.instance().externalCache())).fromURI(proxyEventURI);
    }

    static public UIEvent eventFromEventURI(URI uri) {
        return new UIEvent(new EC(Installation.instance().externalCache())).fromURI(uri);
    }
}
