package gov.nist.asbestos.services.filter;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.log.ChannelDoesNotExistException;
import gov.nist.asbestos.services.restRequests.AddSessionRequest;
import gov.nist.asbestos.services.restRequests.CreateChannelRequest;
import gov.nist.asbestos.services.restRequests.DelSessionRequest;
import gov.nist.asbestos.services.restRequests.DeleteChannelRequest;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.services.restRequests.GetSessionConfigRequest;
import gov.nist.asbestos.services.restRequests.ReplaceChannelRequest;
import gov.nist.asbestos.services.restRequests.SessionConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.IOUtils;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static gov.nist.asbestos.client.Base.Returns.returnPlainTextResponse;

/**
 * The main purpose of this filter is to prevent unauthorized modifications to FTK channels and test sessions.
 */
public class FtkElementsGuardFilter implements Filter {
    private static Logger log = Logger.getLogger(FtkElementsGuardFilter.class.getName());
    private File externalCache = null;

    @Override
    public void init(FilterConfig filterConfig) {
       // log.info("FtkElementsGuardFilter init");

        try {
            ServiceProperties.init();
        } catch (Exception ex) {
            throw new RuntimeException(String.format("ServiceProperties.init() Failed: %s.", ex.toString()));
        }

//        String ec = config.getInitParameter("ExternalCache");
        String ec = System.getProperty("EXTERNAL_CACHE");
        if (ec == null) {
            throw new RuntimeException("Missing EXTERNAL_CACHE system property.");
        }
        setExternalCache(new File(ec));

        // announce location of ExternalCache to other servlets
        filterConfig.getServletContext().setAttribute("ExternalCache", ec);

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
     //   log.info("FtkElementsGuardFilter enter");
        if (request instanceof HttpServletRequest) {

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            String httpVerb = httpServletRequest.getMethod();
            boolean isPost = httpVerb.equalsIgnoreCase("POST");
            boolean isPut = httpVerb.equalsIgnoreCase("PUT");
            boolean isDelete = httpVerb.equalsIgnoreCase("DELETE");

            if (isPost || isPut) {
                FtkHttpServletRequestWrapper myHttpServletRequest = new FtkHttpServletRequestWrapper(httpServletRequest);
                Request requestObj = new Request(myHttpServletRequest, httpServletResponse, externalCache);
                boolean isChannelPostRequest = isPost && CreateChannelRequest.isRequest(requestObj);
                boolean isChannelPutRequest = isPut && ReplaceChannelRequest.isRequest(requestObj);
                boolean isTestSessionPostRequest = isPost && AddSessionRequest.isRequest(requestObj);
                if (isChannelPostRequest || isChannelPutRequest) {
                    validateChannelRequest(response, chain, httpVerb, isPost, isPut, myHttpServletRequest, requestObj);
                    return;
                } else if (isTestSessionPostRequest) {
                    chain.doFilter(myHttpServletRequest, response);
                    return;
                }
            } else if (isDelete) {
                FtkHttpServletRequestWrapper myHttpServletRequest = new FtkHttpServletRequestWrapper(httpServletRequest);
                Request requestObj = new Request(myHttpServletRequest, httpServletResponse, externalCache);
                boolean isChannelDeleteRequest = DeleteChannelRequest.isRequest(requestObj);
                boolean isTestSessionDeleteRequest = DelSessionRequest.isRequest(requestObj);
                if (isChannelDeleteRequest) {
                    validateChannelDeleteRequest(response, chain, myHttpServletRequest, requestObj);
                    return;
                } else if (isTestSessionDeleteRequest) {
                   validateTestSessionDeleteRequest(response, chain, myHttpServletRequest, requestObj);
                   return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static SessionConfig getSessionConfig(File externalCache, String testSessionName) {
        File sessionDir = EC.ftkSessionDir(externalCache, testSessionName);
        File configFile = new File(sessionDir, "config.json");
        return GetSessionConfigRequest.load(configFile);
    }

    private void validateTestSessionDeleteRequest(ServletResponse response, FilterChain chain, FtkHttpServletRequestWrapper myHttpServletRequest, Request requestObj) throws IOException, ServletException {
       // If the test session config is write protected, then exit
        String testSessionName = requestObj.uriParts.get(4);
        if (getSessionConfig(requestObj.externalCache, testSessionName).isSessionConfigLocked()) {
            returnPlainTextResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Session is write protected.");
            return;
        } else {
            chain.doFilter(myHttpServletRequest, response);
            return;
        }
    }


    private void validateChannelDeleteRequest(ServletResponse response, FilterChain chain, FtkHttpServletRequestWrapper myHttpServletRequest, Request channelRequest) throws IOException, ServletException {
        String channelId = channelRequest.uriParts.get(4);
        SimStore simStore = new SimStore(externalCache, SimId.buildFromRawId(channelId));
        try {
            // Channel may not exist
            simStore.open();
        } catch (RuntimeException rex) {
            chain.doFilter(myHttpServletRequest, response);
            return;
        }
        ChannelConfig beforeUpdate = simStore.getChannelConfig();
        if (beforeUpdate.isWriteLocked()) {
            returnPlainTextResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Channel configuration is write protected.");
            return;
        } else {
            SessionConfig sessionConfig = getSessionConfig(channelRequest.externalCache, beforeUpdate.getTestSession());
            if (sessionConfig.isSessionConfigLocked()) {
                if (sessionConfig.isCanRemoveChannel()) {
                    chain.doFilter(myHttpServletRequest, response);
                    return;
                } else {
                    returnPlainTextResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Channel remove feature is protected.");
                    return;
                }
            } else {
                chain.doFilter(myHttpServletRequest, response);
                return;
            }
        }
    }

    private void validateChannelRequest(ServletResponse response, FilterChain chain, String httpVerb, boolean isPost, boolean isPut, FtkHttpServletRequestWrapper myHttpServletRequest, Request channelRequest) throws IOException, ServletException {
        log.info(String.format("Channel Control %s (FtkElementsGuardFilter): %s" , httpVerb, channelRequest.uri));

        String rawRequest = IOUtils.toString(myHttpServletRequest.getInputStream(), Charset.defaultCharset());   // json
        ChannelConfig channelConfigInRequest = ChannelConfigFactory.convert(rawRequest);

        SimStore simStore = new SimStore(externalCache,
                new SimId(new TestSession(channelConfigInRequest.getTestSession()),
                        channelConfigInRequest.getChannelName(),
                        channelConfigInRequest.getActorType(),
                        channelConfigInRequest.getEnvironment(),
                        true));
        try {
            simStore.open();
            if (isPost) {
                returnPlainTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Channel configuration already exists.");
                return;
            }
        } catch (ChannelDoesNotExistException ex) {
            if (isPost) { // Expected exception
                SessionConfig sessionConfig = getSessionConfig(channelRequest.externalCache, channelConfigInRequest.getTestSession());
                if (sessionConfig.isSessionConfigLocked()) {
                    if (sessionConfig.isCanAddChannel()) {
                        chain.doFilter(myHttpServletRequest, response);
                        return;
                    } else {
                        returnPlainTextResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "New channels cannot be created in a locked test session.");
                        return;
                    }
                } else {
                    chain.doFilter(myHttpServletRequest, response);
                    return;
                }
            } else if (isPut) { // Channel configuration must exist if replacing
                returnPlainTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Channel configuration does not exist.");
                return;
            }
        }

        ChannelConfig beforeUpdate = simStore.getChannelConfig();
        if (beforeUpdate.isWriteLocked()) {
            // Only way through this is to send a request through the accessGuard servlet mapping, which has nothing to do with this GuardFilter.
            returnPlainTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Channel configuration is write protected.");
            return;
        } else {
            chain.doFilter(myHttpServletRequest, response);
            return;
        }
    }


    @Override
    public void destroy() {

    }


    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
     //   log.fine("Asbestos Proxy init EC is " + externalCache.getPath());
    }

}
