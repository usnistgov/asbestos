package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.io.IOUtils;
import java.util.logging.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
// 0 - empty
// 1 - app context
// 2  "rw" or "accessGuard"
// 3 "channel"
// 4 "create"
// Create a channel based on JSON configuration in request

public class CreateChannelRequest {
    private static Logger log = Logger.getLogger(CreateChannelRequest.class.getName());

    protected Request request;
    protected String rawRequest;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 5) {
            int channelIndex = 3;
            int createIndex = 4;

            return "channel".equals(request.uriParts.get(channelIndex)) && "create".equals(request.uriParts.get(createIndex));
        }
        return false;
    }

    public CreateChannelRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("CreateChannel");
        if (rawRequest == null) {
            rawRequest = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
            log.fine(()->"CREATE Channel " + rawRequest);
        }
        ChannelConfig channelConfig = ChannelConfigFactory.convert(rawRequest);

        if ("fhir".equalsIgnoreCase(channelConfig.getChannelType()) && channelConfig.isLogMhdCapabilityStatementRequest()) {
            channelConfig.setLogMhdCapabilityStatementRequest(false);
        }


        boolean isInvalidChannelName = ! SimStore.isValidCharsPattern().matcher(channelConfig.asChannelId()).matches()
                || SimStore.isReservedNamesPattern(null).matcher(channelConfig.getChannelName()).matches();

        if (isInvalidChannelName) {
            String error = "Invalid channel name. Check if name contains an illegal character or is a reserved name.";
            log.warning(error + ": " +  channelConfig.asChannelId());
            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(error);
            request.setStatus((request.resp.SC_BAD_REQUEST));
            return;
        }

        if (! isMhdVersionValid(channelConfig.getCcFhirIgName())) {
            String error = "Invalid mhdVersion.";
            log.warning(error + ": " +  channelConfig.asChannelId());
            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(error);
            request.setStatus((request.resp.SC_BAD_REQUEST));
            return;
        }

        SimStore simStore = new SimStore(request.externalCache,
                new SimId(new TestSession(channelConfig.getTestSession()),
                        channelConfig.asChannelId(),
                        channelConfig.getActorType(),
                        channelConfig.getEnvironment(),
                        true));

        simStore.create(channelConfig);
        log.info("Channel " + simStore.getChannelId().toString() + " created (type " + simStore.getActorType() + ")" );

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(rawRequest);

        request.setStatus((simStore.isNewlyCreated() ? request.resp.SC_CREATED : request.resp.SC_OK));
    }

    private static boolean isMhdVersionValid(String[] mhdVersions) {
         if (mhdVersions == null) { /* null means nothing is specified, which is valid since mhdVersion is optional: Oldest IG will be the default. */
             return true;
         }
         // If a mhdVersion is indeed specified, make sure it is mappable to the enum
         try {
             if (mhdVersions != null  && mhdVersions.length > 0) {
                 long count = Arrays.stream(mhdVersions).map(e -> MhdIgImplEnum.find(e)).count();
                 if (mhdVersions.length == count)
                     return true;
             } else {
                 return true; // 0 length is OK
             }
         } catch (Exception ex) {
            return false;
         }
         return false;
    }
}
