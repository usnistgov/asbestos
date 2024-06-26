package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.asbestosProxy.channel.XdsToolkitConnection;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.toolkit.toolkitServicesCommon.SimConfig;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.Optional;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "xdsheartbeat"
// load simConfig for xds channel to prove XDS Toolkit is healthy

public class XdsHeartbeat {
    private static Logger log = Logger.getLogger(XdsHeartbeat.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(3).equals("xdsheartbeat");
    }

    public XdsHeartbeat(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    class HeartBeat {
        String addr;
        boolean responding;
    }

    public void run() {
        log.info("xdsheartbeat");

        HeartBeat heartBeat = new HeartBeat();
        try {
            request.resp.setStatus(request.resp.SC_OK);
            XdsToolkitConnection conn = new XdsToolkitConnection(request.externalCache, "default");
            Optional<SimConfig> simConfigOptional = conn.get("xds");
            heartBeat.addr = conn.getXdsToolkitBase();
            //msg("XDS Toolkit: xds sim exists on " + conn.getXdsToolkitBase());
            //msg("XDS Toolkit: xds sim does not exist on " + conn.getXdsToolkitBase());
            //request.resp.setStatus(request.resp.SC_SERVICE_UNAVAILABLE);
            heartBeat.responding = simConfigOptional.isPresent();
        } catch (IOException e) {
            heartBeat.responding = false;
            //msg(e.getMessage());
        }
        request.returnObject(heartBeat);
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
