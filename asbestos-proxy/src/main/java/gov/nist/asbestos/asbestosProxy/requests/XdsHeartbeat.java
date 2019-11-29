package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.channel.XdsToolkitConnection;
import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.toolkit.toolkitApi.DocumentRegRep;
import gov.nist.toolkit.toolkitApi.SimulatorBuilder;
import gov.nist.toolkit.toolkitApi.ToolkitServiceException;
import gov.nist.toolkit.toolkitServicesCommon.SimConfig;
import gov.nist.toolkit.toolkitServicesCommon.resource.SimIdResource;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "xdsheartbeat"
// load simConfig for xds channel to prove XDS Toolkit is healthy

public class XdsHeartbeat {
    private static Logger log = Logger.getLogger(XdsHeartbeat.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(3).equals("xdsheartbeat");
    }

    public XdsHeartbeat(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("xdsheartbeat");


        try {
            XdsToolkitConnection conn = new XdsToolkitConnection(request.externalCache, "default");
            Optional<SimConfig> simConfigOptional = conn.get("xds");
            if (simConfigOptional.isPresent()) {
                msg("XDS Toolkit: xds sim responds on " + conn.getXdsToolkitBase());
            } else {
                msg("XDS Toolkit: xds sim does not respond on " + conn.getXdsToolkitBase());
                request.resp.setStatus(request.resp.SC_SERVICE_UNAVAILABLE);
            }
        } catch (IOException e) {
            msg(e.getMessage());
        }
    }

    private void msg(String msg)  {
        try {
            request.resp.getOutputStream().write(msg.getBytes());
            request.resp.setStatus(request.resp.SC_OK);
        } catch (Exception e) {
            //
        }
    }

}
