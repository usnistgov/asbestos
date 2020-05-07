package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
    private String query = null;

    public Request(HttpServletRequest req, HttpServletResponse resp, File externalCache) {
        this.req = req;
        this.resp = resp;
        String qstring = req.getQueryString();
        //isJson = "_format=json".equals(qstring);
        if (qstring != null) {
            isJson = qstring.contains("_format=json");
            isGzip = qstring.contains("_gzip=true");
        }
        this.externalCache = externalCache;
        uri = Common.buildURI(req);
        uriParts = Arrays.asList(uri.getPath().split("/"));
        query  = uri.getQuery();
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

    public URL getFullUrl()  {
        String url = uri.toString();
//        String query = req.getQueryString();
//        if (query != null && !query.equals(""))
//            url = url + "?" + query;
        if (url.startsWith("/asbestos"))
            url = url.substring("/asbestos".length());
        url = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) + url;
        if (query != null && !query.equals("") && !url.contains("?"))
            url = url + "?" + query;
        try {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
            return new URL(url);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            return null;
        }
    }

    public String getParm(String name) {
        String query = this.query; //req.getQueryString();
        try {
            query = URLDecoder.decode(query, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        if (query == null)
            return null;
        int parmi = query.indexOf(name + "=");
        if (parmi == -1)
            return null;
        int parmend = query.indexOf(";", parmi);
        if (parmend == -1)
            parmend = query.length();
        int parmstart = query.indexOf("=", parmi);
        if (parmstart == -1)
            return null;
        parmstart++;
        if (parmend <= parmstart)
            return null;
        return query.substring(parmstart, parmend);

    }
}
