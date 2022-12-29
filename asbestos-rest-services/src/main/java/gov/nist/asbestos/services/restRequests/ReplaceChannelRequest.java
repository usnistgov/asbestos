package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import static gov.nist.asbestos.client.Base.Returns.returnPlainTextResponse;

// 0 - empty
// 1 - app context
// 2  "rw" or "accessGuard"
// 3 - "channel"
// 4 - "channelID"
// Create a channel based on JSON configuration in request


public class ReplaceChannelRequest extends CreateChannelRequest {
    private static Logger log = Logger.getLogger(CreateChannelRequest.class.getName());

    public ReplaceChannelRequest(Request request) throws IOException {
        super(request);
        request.setType(this.getClass().getSimpleName());
    }

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 5) {
            String uriPart3 = request.uriParts.get(3);
            String uriPart4 = request.uriParts.get(4);
            return "channel".equals(uriPart3) && uriPart4.contains("__") /* channel Id format */;
        }
        return false;
    }

    @Override
    public void run() throws IOException {
        String channelId = request.uriParts.get(4);
        ChannelConfig channelConfigAsPerTheUri;

        try {
            channelConfigAsPerTheUri = ChannelControl.channelConfigFromChannelId(request.externalCache, channelId);
            log.fine("REPLACE Channel " + rawRequest);
            ChannelConfig channelConfigAsPerBody = ChannelConfigFactory.convert(rawRequest);

            /*
            if (channelConfigAsPerTheUri.getMhdVersions() == null || channelConfigAsPerTheUri.getMhdVersions().length == 0) {
                returnPlainTextResponse(request.resp, HttpServletResponse.SC_BAD_REQUEST, "mhdVersion must be selected.");
                return;
            }
             */

            if (!channelConfigAsPerTheUri.asChannelId().equals(channelConfigAsPerBody.asChannelId())) { // The supplied entity must match the resource at URI being replaced
               returnPlainTextResponse(request.resp, HttpServletResponse.SC_BAD_REQUEST, "Provided request body ChannelID does not match the ChannelID in the request URI.");
               return;
            }
        } catch (Throwable e) {
            request.notFound();
            return;
        }

        log.info("Calling CREATE to REPLACE");
        super.run();
    }
}
