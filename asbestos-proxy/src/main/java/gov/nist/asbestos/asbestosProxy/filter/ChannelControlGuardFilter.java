package gov.nist.asbestos.asbestosProxy.filter;

import gov.nist.asbestos.asbestosProxy.requests.CreateChannelRequest;
import gov.nist.asbestos.asbestosProxy.requests.DeleteChannelRequest;
import gov.nist.asbestos.asbestosProxy.requests.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
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
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {
                ByteArrayInputStream bais = new ByteArrayInputStream(body);

                @Override
                public int read() throws IOException {
                    return bais.read();
                }
            };
        }

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("ChannelControlGuardFilter init");


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
        log.info("ChannelControlGuardFilter enter");
        if (request instanceof HttpServletRequest) {

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

                if (httpServletRequest.getMethod().equalsIgnoreCase("POST")) {
                    MyHttpServletRequestWrapper myHttpServletRequest = new MyHttpServletRequestWrapper(httpServletRequest);
                    Request channelRequest = new Request(myHttpServletRequest, httpServletResponse, externalCache);
                    if (CreateChannelRequest.isRequest(channelRequest)) {
                        log.info("Channel Control POST " + channelRequest.uri);


                        String rawRequest = IOUtils.toString(myHttpServletRequest.getInputStream(), Charset.defaultCharset());   // json
                        ChannelConfig channelConfigInRequest = ChannelConfigFactory.convert(rawRequest);

                        SimStore simStore = new SimStore(externalCache,
                                new SimId(new TestSession(channelConfigInRequest.getTestSession()),
                                        channelConfigInRequest.getChannelId(),
                                        channelConfigInRequest.getActorType(),
                                        channelConfigInRequest.getEnvironment(),
                                        true));
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
                } else if (httpServletRequest.getMethod().equalsIgnoreCase("DELETE")) {
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
        log.debug("Asbestos Proxy init EC is " + externalCache.getPath());
    }

}
