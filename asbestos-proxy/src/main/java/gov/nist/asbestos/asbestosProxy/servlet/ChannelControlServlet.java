package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChannelControlServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(ChannelControlServlet.class);
    private File externalCache = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("ChannelControlServlet init");
        if (externalCache == null) {
            String ec = (String) config.getServletContext().getAttribute("ExternalCache");
            externalCache = new File(ec);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("Channel Control doPost " + uri);
        SimStore simStore;
        try {
            List<String> uriParts1 = Arrays.asList(uri.getPath().split("/"));
            List<String> uriParts = new ArrayList<>(uriParts1);  // so parts are deletable

            if (uriParts.size() == 3) {
                // 0 - empty
                // 1 - app context
                // 2 - "channel"
                // Create a channel based on JSON configuration in request

                String rawRequest = IOUtils.toString(req.getInputStream(), Charset.defaultCharset());   // json
                log.debug("CREATE Channel " + rawRequest);
                ChannelConfig channelConfig = ChannelConfigFactory.convert(rawRequest);
                simStore = new SimStore(externalCache,
                        new SimId(new TestSession(channelConfig.getTestSession()),
                                channelConfig.getChannelId(),
                                channelConfig.getActorType(),
                                channelConfig.getEnvironment(),
                                true));

                simStore.create(channelConfig);
                log.info("Channel " + simStore.getChannelId().toString() + " created (type " + simStore.getActorType() + ")" );

                resp.setContentType("application/json");
                resp.getOutputStream().print(rawRequest);


                resp.setStatus((simStore.isNewlyCreated() ? resp.SC_CREATED : resp.SC_OK));
                log.info("OK");
            }

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("Channel Control doGet " + uri);
        try {
            List<String> uriParts1 = Arrays.asList(uri.getPath().split("/"));
            List<String> uriParts = new ArrayList<>(uriParts1);  // so parts are deletable

            if (uriParts.size() == 3 && "channel".equalsIgnoreCase(uriParts.get(2))) {
                // 0 - empty
                // 1 - app context
                // 2 - "channel"
                // Return list of channel IDs

                SimStore simStore = new SimStore(externalCache);
                List<String> ids = simStore.getChannelIds();

                String json = new Gson().toJson(ids);

                resp.setContentType("application/json");
                resp.getOutputStream().print(json);

                resp.setStatus(resp.SC_OK);
                log.info("OK");
            } else if (uriParts.size() == 4 && "channel".equalsIgnoreCase(uriParts.get(2))) {
                // 0 - empty
                // 1 - app context
                // 2 - "channel"
                // 3 - channelID
                String channelId = uriParts.get(3);
                ChannelConfig channelConfig;

                try {
                    channelConfig = channelConfigFromChannelId(externalCache, channelId);
                } catch (Throwable e) {
                    resp.setStatus(resp.SC_NOT_FOUND);
                    return;
                }
                String configString = ChannelConfigFactory.convert(channelConfig);

                resp.setContentType("application/json");
                resp.getOutputStream().print(configString);

                resp.setStatus(resp.SC_OK);
            } else {
                throw new Exception("Invalid request - do not understand URI " + uri);
            }

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }

    static public ChannelConfig channelConfigFromChannelId(File externalCache, String channelId) {
        SimId simId = SimId.buildFromRawId(channelId);
        SimStore simStore = new SimStore(externalCache, simId);
        simStore.open();
        return simStore.getChannelConfig();
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        URI uri = Common.buildURI(req);
        log.info("Channel Control doPost " + uri);
        try {
            List<String> uriParts1 = Arrays.asList(uri.getPath().split("/"));
            List<String> uriParts = new ArrayList<>(uriParts1);  // so parts are deletable

            if (uriParts.size() == 4 && "channel".equalsIgnoreCase(uriParts.get(2))) {
                // 0 - empty
                // 1 - app context
                // 2 - "channel"
                // 3 - channelID  (testSession__id)
                String channelId = uriParts.get(3);

                SimId simId = SimId.buildFromRawId(channelId);
                SimStore simStore = new SimStore(externalCache, simId);
                simStore.deleteSim();
            } else {
                throw new Exception("Invalid request - do not understand URI " + uri);
            }

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
    }
}
