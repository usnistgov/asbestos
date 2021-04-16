package gov.nist.asbestos.services.filter;

import gov.nist.asbestos.client.log.ChannelDoesNotExistException;
import gov.nist.asbestos.services.restRequests.CreateChannelRequest;
import gov.nist.asbestos.services.restRequests.DeleteChannelRequest;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.services.restRequests.ReplaceChannelRequest;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ChannelControlGuardFilter implements Filter {
    private static Logger log = Logger.getLogger(ChannelControlGuardFilter.class);
    private File externalCache = null;

    private class MyHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private byte[] body;

        public MyHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
            try {
                body = IOUtils.toByteArray(request.getInputStream());
            } catch (IOException ex) {
                body = new byte[0];
            }
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                ByteArrayInputStream bais = new ByteArrayInputStream(body);

                @Override
                public int read()  {
                    return bais.read();
                }
            };
        }

    }

    @Override
    public void init(FilterConfig filterConfig) {
       // log.info("ChannelControlGuardFilter init");

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
     //   log.info("ChannelControlGuardFilter enter");
        if (request instanceof HttpServletRequest) {

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            String httpVerb = httpServletRequest.getMethod();
            boolean isPost = httpVerb.equalsIgnoreCase("POST");
            boolean isPut = httpVerb.equalsIgnoreCase("PUT");
            boolean isDelete = httpVerb.equalsIgnoreCase("DELETE");

            if (isPost || isPut) {
                MyHttpServletRequestWrapper myHttpServletRequest = new MyHttpServletRequestWrapper(httpServletRequest);
                Request channelRequest = new Request(myHttpServletRequest, httpServletResponse, externalCache);
                boolean isValidPostRequest = isPost && CreateChannelRequest.isRequest(channelRequest);
                boolean isValidPutRequest = isPut && ReplaceChannelRequest.isRequest(channelRequest);
                if (isValidPostRequest || isValidPutRequest) {
                    log.info(String.format("Channel Control %s (ChannelControlGuardFilter): %s" , httpVerb, channelRequest.uri));

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
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Channel configuration already exists.");
                            return;
                        }
                    } catch (ChannelDoesNotExistException ex) {
                        if (isPost) { // Expected exception
                            chain.doFilter(myHttpServletRequest, response);
                            return;
                        } else if (isPut) { // Channel configuration must exist if replacing
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Channel configuration does not exist.");
                            return;
                        }
                    }

                    ChannelConfig beforeUpdate = simStore.getChannelConfig();
                    if (beforeUpdate.isWriteLocked()) {
                        // Only way through this is to send a request through the channelGuard servlet.
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Channel configuration is write protected.");
                        return;
                    } else {
                        chain.doFilter(myHttpServletRequest, response);
                        return;
                    }
                }
            } else if (isDelete) {
                MyHttpServletRequestWrapper myHttpServletRequest = new MyHttpServletRequestWrapper(httpServletRequest);
                Request channelRequest = new Request(myHttpServletRequest, httpServletResponse, externalCache);
                if (DeleteChannelRequest.isRequest(channelRequest)) {
                    String channelId = channelRequest.uriParts.get(3);
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
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Channel configuration is write protected.");
                        return;
                    } else {
                        chain.doFilter(myHttpServletRequest, response);
                        return;
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }


    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
     //   log.debug("Asbestos Proxy init EC is " + externalCache.getPath());
    }

}
