package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
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

    public CreateChannelRequest(Request request) throws IOException {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        this.rawRequest = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
    }

    public void run() throws IOException {
        request.announce("CreateChannel");
        log.fine(()->"CREATE Channel " + rawRequest);
        ChannelConfig channelConfig = verifyChannelConfig(ChannelConfigFactory.convert(rawRequest));
        if (channelConfig == null) return;

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

    @Nullable
    private ChannelConfig verifyChannelConfig(ChannelConfig channelConfig) throws IOException {

        if (FtkChannelTypeEnum.fhir.equals(channelConfig.getChannelType()) && channelConfig.isLogMhdCapabilityStatementRequest()) {
            channelConfig.setLogMhdCapabilityStatementRequest(false);
        }

        boolean isInvalidChannelName = ! SimStore.isValidCharsPattern().matcher(channelConfig.getTestSession()).matches()
                || ! SimStore.isValidCharsPattern().matcher(channelConfig.getChannelName()).matches()
                || SimStore.isReservedNamesPattern(null).matcher(channelConfig.getChannelName()).matches();

        if (isInvalidChannelName) {
            String warningMessage = "Invalid channel name. Check if name contains an illegal character or is a reserved name.";
            log.warning(warningMessage + ": " +  channelConfig.asChannelId());
            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(warningMessage);
            request.setStatus((request.resp.SC_BAD_REQUEST));
            return null;
        }

        if (! isFhirIgNameValid(channelConfig)) {
            String warningMessage = "Invalid fhirIgName property in " + channelConfig.asChannelId();
            log.warning(warningMessage);
            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(warningMessage);
            request.setStatus((request.resp.SC_BAD_REQUEST));
            return null;
        }
        return channelConfig;
    }

    private static boolean isFhirIgNameValid(ChannelConfig channelConfig) {
        String[] fhirIgNames = channelConfig.getCcFhirIgName();
         if (fhirIgNames == null) { /* null means nothing is specified, which is valid since this is optional: Oldest IG may be the default according to the channel type. */
             return true;
         }
         // If a fhirIgName was indeed specified, make sure it is mappable to an implementation enum
         try {
             if (fhirIgNames != null  && fhirIgNames.length > 0) {
                 Function<String,Enum> f = null;
                 final FtkChannelTypeEnum channelType = channelConfig.getChannelType();
                 Optional<String> igName = channelType.getChannelTypeIgTestCollection().getIgTestCollections().stream()
                         .map(s -> s.getIgName().toString())
                         .filter(s -> Arrays.asList(fhirIgNames).contains(s))
                         .findAny();
                 if (igName.isPresent())
                     return true;
             } else {
                 return true; // 0 length is OK
             }
         } catch (Exception ex) {
             log.severe("isFhirIgNameValid exception: " + ex.toString());
            return false;
         }
         return false;
    }
}
