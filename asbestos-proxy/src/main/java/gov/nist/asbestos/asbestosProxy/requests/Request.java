package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.http.support.Common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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
        setOptions(qstring);
        setExternalCache(externalCache);
        uri = Common.buildURI(req);
        setUriParts(uri);
    }

    public Request(String url, File externalCache) throws URISyntaxException {
       setOptions(url);
       setExternalCache(externalCache);
       uri = new URI(url);
       setUriParts(uri);
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

    private void setUriParts(URI uri) {
        uriParts = Arrays.asList(uri.getPath().split("/"));
    }

    private void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        ec = new EC(externalCache);
    }

    private void setOptions(String qstring) {
        if (qstring != null) {
            isJson = qstring.contains("_format=json");
            isGzip = qstring.contains("_gzip=true");
        }
    }

}
