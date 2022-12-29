package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context (asbestos)
// 2 - "log"
// 3 - "native"
// url param is to be translated

import com.google.common.base.Strings;
import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.client.resolver.Ref;
import java.util.logging.Logger;

import java.io.IOException;

public class GetNativeUrlRequest {
    private static Logger log = Logger.getLogger(GetNativeUrlRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "native".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetNativeUrlRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetNativeUrlRequest");
        String urlIn = request.getParm("url");
        if (urlIn == null) {
            request.badRequest();
            return;
        }
        String[] urlInParts = urlIn.split("/");
        if (urlInParts.length < 8) {
            request.badRequest();
            return;
        }
        if (!"proxy".equals(urlInParts[4])) {
            request.badRequest();
            return;
        }
        String channelId = urlInParts[5];
        ChannelConfig channelConfig;

        try {
            channelConfig = ChannelControl.channelConfigFromChannelId(request.externalCache, channelId);
        } catch (Throwable e) {
            request.notFound();
            return;
        }

        if (FtkChannelTypeEnum.fhir.equals(channelConfig.getChannelType())) {
            Ref ref = new Ref(urlIn);
            ref = ref.rebase(channelConfig.getFhirBase());
            request.resp.setContentType("text/plain");
            request.resp.getOutputStream().print(ref.toString());
            request.ok();
            return;
        }
        request.notFound();
    }
}
