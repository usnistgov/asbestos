package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.testEngine.engine.FtkInternalRequestCode;
import gov.nist.asbestos.testcollection.VARIABLE_PROP_REFERENCE_PARTS;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


// 0 - empty
// 1 - web server application context
// 2 - "engine"
// 3 - the FTK_FUNCTION_CODE
// 4 - channelId
// 5 - testCollectionId
// 6 - testId
// Example: https://fhirtoolkit.test:9743/asbestos/engine/theRequestCode/default__limited/MHD_DocumentRecipient_minimal/Missing_DocumentManifest?validationFhirServerChannelId=


public class GetFtkFunctionCodeResultRequest {
    private static Logger log = Logger.getLogger(GetFtkFunctionCodeResultRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 7) {
            String uriPart3 = request.uriParts.get(3);
            return FtkInternalRequestCode.FTK_FUNCTION_CODE.getCode().equals(uriPart3);
        }
        return false;

    }

    public GetFtkFunctionCodeResultRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce(this.getClass().getName());
        Map<String, String> paramsMap = request.getParametersMap();

        List<Resource> result = new ArrayList<>();
        String fn = paramsMap.get(FtkInternalRequestCode.FTK_FUNCTION_CODE_FN_PARAM.getCode());
        if (fn != null) {
            try {
                FtkInternalRequestCode fnCode = FtkInternalRequestCode.find(fn);
                switch (fnCode) {
                    case FTK_FUNCTION_CODE_FN_GET_CHANNEL_FHIR_BASE:
                    case FTK_FUNCTION_CODE_FN_GET_CHANNEL_BASE:
                    case FTK_FUNCTION_CODE_FN_GET_VALIDATION_BASE:
                        result.add(channelFn(fnCode, paramsMap));
                    break;
                }
            } catch (FnException ex) {
                request.badRequest("Malformed request parameters.");
                return;
            }
        }

        if (result.isEmpty()) {
            String error = String.format("Empty result for function %s", fn);
            request.serverError(error);
            return;
        }

        try {
            Bundle outBundle = ParserBase.bundleWith(result);
            String jsonStr = ParserBase.encode(outBundle, Format.JSON);

            request.resp.setContentType("application/json");
            request.resp.getOutputStream().print(jsonStr);
            request.ok();
        } catch (Exception ex) {
            request.serverError(ex);
            return;
        }
    }

    private Binary channelFn(FtkInternalRequestCode fnCode, Map<String, String> paramsMap) throws FnException {
        if (fnCode == null) {
            request.badRequest(String.format("Null fnCode param."));
            throw new FnException();
        }

        String result = null;

        if (fnCode.equals(FtkInternalRequestCode.FTK_FUNCTION_CODE_FN_GET_CHANNEL_FHIR_BASE)) {
            String channelId = getChannelIdFromParam(paramsMap);
            ChannelConfig channelConfig = getChannelConfig(channelId);
            result = channelConfig.getFhirBase();
        } else if (fnCode.equals(FtkInternalRequestCode.FTK_FUNCTION_CODE_FN_GET_CHANNEL_BASE)) {
            String channelId = getChannelIdFromParam(paramsMap);
            ChannelConfig channelConfig = getChannelConfig(channelId);
            result = channelConfig.getProxyURI().toString();
        } else if (fnCode.equals(FtkInternalRequestCode.FTK_FUNCTION_CODE_FN_GET_VALIDATION_BASE)) {
            String channelId = request.uriParts.get(4);
            ChannelConfig channelConfig = getChannelConfig(channelId);
            result = channelConfig.getValidationURI().toString();
        }

        if (result == null) {
            request.serverError();
            throw new FnException();
        }

        Binary b = new Binary();
        b.setContentType("text/plain");
        b.setData(result.getBytes());

        return b;
    }

    private String getChannelIdFromParam(Map<String, String> paramsMap) {
        String channelId = paramsMap.get(FtkInternalRequestCode.FTK_FUNCTION_CODE_CHANNELID_PARAM.getCode());
        if (VARIABLE_PROP_REFERENCE_PARTS.DefaultToGlobalServiceProperty.getToken().equals(channelId)) {
            channelId = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_VALIDATION_CHANNEL_ID);
        }
        return channelId;
    }

    private ChannelConfig getChannelConfig(String channelId) throws FnException {
        try {
            SimId simId = SimId.buildFromRawId(channelId);
            if (!simId.isValid()) {
                request.badRequest("Invalid SimId.");
                throw new FnException();
            }
        } catch (Exception ex) {
            request.serverError(ex);
            throw new FnException();
        }

        try {
            return ChannelControl.channelConfigFromChannelId(request.externalCache, channelId);
        } catch (Throwable e) {
            request.notFound();
            throw new FnException();
        }
    }

    private class FnException extends Throwable {
    }
}
