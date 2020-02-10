package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.http.support.Common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public URI uri;
    public List<String> uriParts;
    public File externalCache;
    public EC ec;
    public String testSession;
    public String channelId;
    public boolean isJson = true;
    public boolean isGzip = false;

    public Request(HttpServletRequest req, HttpServletResponse resp, File externalCache) {
        this.req = req;
        this.resp = resp;
        String qstring = req.getQueryString();
        //isJson = "_format=json".equals(qstring);
        isJson = qstring.contains("_format=json");
        isGzip = qstring.contains("_gzip=true");
        this.externalCache = externalCache;
        uri = Common.buildURI(req);
        uriParts = Arrays.asList(uri.getPath().split("/"));
        ec = new EC(externalCache);
    }

    public String fullChannelId() { return testSession + "__" + channelId; }

    public HttpSession getSession() {
        return req.getSession();
    }

    public void parseChannelName(int part) {
        String value = uriParts.get(part);
        String[] parts = value.split("__");
        if (parts.length == 2) {
            testSession = parts[0];
            channelId = parts[1];
        }
    }

}
