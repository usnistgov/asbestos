package gov.nist.asbestos.client.Base;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hl7.fhir.r4.model.BaseResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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
    private Map<String, String> parametersMap;
    private String type;
    private static final Logger log = Logger.getLogger(Request.class.getName());

    public Request(HttpServletRequest req, HttpServletResponse resp, File externalCache) {
        this.req = req;
        this.resp = resp;
        String qstring = req.getQueryString();
        setOptions(qstring);
        setExternalCache(externalCache);
        uri = Common.buildURI(req);
        parametersMap = new Ref(uri).getParametersAsMap(qstring);
        setUriParts(uri);
        query  = uri.getQuery();
    }

    public Request(String url, File externalCache) throws URISyntaxException {
       setOptions(url);
       setExternalCache(externalCache);
       uri = new URI(url);
       parametersMap = new Ref(uri).getParametersAsMap();
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

    public URL getFullUrl()  {
        String url = uri.toString();
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
        String query = this.query;
        if (query == null)//req.getQueryString();
            return null;
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

    public String segment(int i) {
        if (i >= uriParts.size())
            return null;
        return uriParts.get(i);
    }

    public void setStatus(int status) {
        if (status == 200) {
            log.log(Level.FINE, ()->String.format("Status %d: URI=%s", status, uri));
        } else {
            log.severe(String.format("%s Error Status: %d, URI=%s", getType(), status, uri));
        }
        resp.setStatus(status);
    }

    public void badRequest() {
        log.severe("Do not understand " + uri.toString());
        setStatus(resp.SC_BAD_REQUEST);
    }

    public void badRequest(String msg) {
        log.severe(msg);
        setStatus(resp.SC_BAD_REQUEST);
    }

    public void notFound() {  setStatus(resp.SC_NOT_FOUND);}

    public void serverError() { setStatus(resp.SC_INTERNAL_SERVER_ERROR);}

    public void serverError(Throwable t) {
        log.log(Level.SEVERE, String.format("%s Error Message:  %s", getType(), t.toString()), t);
        setStatus(resp.SC_INTERNAL_SERVER_ERROR);
    }

    public void ok() { setStatus(resp.SC_OK); }

    public void returnResource(BaseResource resource) { Returns.returnResource(resp, resource); ok(); }

    public void returnString(String str) { Returns.returnString(resp, str); }

    public void returnObject(Object o) { Returns.returnObject(resp, o); }

    public void returnValue(String value) { Returns.returnValue(resp, value); }

    public void returnList(List<String> list) {  Returns.returnList(resp, list); }

    public void announce(String name) {
        log.info("*** " + name + " " + uri.toString());
    }

    public Map<String, String> getParametersMap() {
        return parametersMap;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
