package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.testEngine.engine.FtkInternalRequestCode;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

// 0 - empty
// 1 - web server application context
// 2 - "engine"
// 3 - "getFtkChannelFhirBase"
// 4 - channelId
// 5 - testCollectionId
// 6 - testId
// Example: https://fhirtoolkit.test:9743/asbestos/engine/theRequestCode/default__limited/MHD_DocumentRecipient_minimal/Missing_DocumentManifest?validationFhirServerChannelId=


public class GetChannelConfigAsAResourceRequest {
    private static Logger log = Logger.getLogger(GetChannelConfigAsAResourceRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 7) {
            String uriPart3 = request.uriParts.get(3);
            return FtkInternalRequestCode.GET_FTK_CHANNEL_FHIR_BASE.getCode().equals(uriPart3);
        }
        return false;

    }

    public GetChannelConfigAsAResourceRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce(this.getClass().getName());
        Map<String, String> paramsMap = request.getParametersMap();
        String channelId = paramsMap.get("FhirValidationChannelId");

        try {
            SimId simId = SimId.buildFromRawId(channelId);
            if (! simId.isValid()) {
                request.badRequest("Invalid SimId.");
                return;
            }
        } catch (Exception ex) {
            request.serverError(ex);
            return;
        }

        ChannelConfig channelConfig;

        try {
            channelConfig = ChannelControl.channelConfigFromChannelId(request.externalCache, channelId);
        } catch (Throwable e) {
            request.notFound();
            return;
        }

        try {
            Binary b = new Binary();
            b.setContentType("text/plain");
            b.setData(channelConfig.getFhirBase().getBytes());
            Bundle outBundle = ParserBase.bundleWith(Arrays.asList(b));
            String jsonStr = ParserBase.encode(outBundle, Format.JSON);

            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(jsonStr);
            request.ok();
        } catch (Exception ex) {
            request.serverError(ex);
            return;
        }
    }
}
