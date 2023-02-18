package gov.nist.asbestos.services.restRequests;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.client.channel.IgNameConstants;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.resolver.ChannelUrl;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.services.servlet.ProxyServlet;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// 0 - empty
// 1 - web server application context
// 2 - "ftkValidate"
// 3 - channelId
// 4 - resourceType
// 5 - $validate
// See also http://hl7.org/fhir/valueset-testscript-operation-codes.html
// Example: https://fhirtoolkit.test:9743/asbestos/ftkValidate/channelId/${resourceType}/$validate?profile=${igResourceProfile}&igName=${igName}&logEvent=true

public class ValidateFhirResourceRequest {
    private static Logger logger = Logger.getLogger(ValidateFhirResourceRequest.class.getName());

    private Request request;
    private String rawRequest;


    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 6) {
            String uriPart2 = request.uriParts.get(2);
            return "ftkValidate".equals(uriPart2);
        }
        return false;

    }

    public ValidateFhirResourceRequest(Request request) throws IOException {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        this.rawRequest = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
    }

    public void run() throws IOException {
        request.announce(this.getClass().getName());

        try {
            Map<String, String> paramsMap = request.getParametersMap();

            final String profile = paramsMap.get("profile");
            final String igName = paramsMap.get("igName");
            final String logEvent = paramsMap.get("logEvent");


            IgNameConstants igNameConstant = IgNameConstants.find(igName);
            if (igNameConstant == null) {
                String error = String.format("%s igName not found.", igName);
                request.badRequest(error);
                return;
            }

            FhirValidator fhirValidator = FtkChannelTypeEnum.findValidator(igNameConstant);
            if (fhirValidator == null) {
                String error = String.format("Null validator for %s.", igNameConstant.getIgName());
                request.serverError(error);
                return;
            }

            ValidationOptions validationOptions = new ValidationOptions();
            if (profile != null && !"".equals(profile)) {
                validationOptions.addProfile(profile);
            }

            ValidationResult validationResult = fhirValidator.validateWithResult(rawRequest, validationOptions);
            OperationOutcome oo = (OperationOutcome) validationResult.toOperationOutcome();

            Headers h = Common.getRequestHeaders(request.req);
            if (! h.hasContentType()) {
                request.badRequest("Request must have content-type header.");
                return;
            }

            String contentTypeValue = h.getContentType().getValue();
            if (contentTypeValue == null) {
                request.badRequest("Request content-type header value cannot be Null.");
                return;
            }

            if (! Format.isFormat(contentTypeValue)) {
                request.badRequest(String.format("Do not understand Request content-type header value %s.", contentTypeValue ));
                return;
            }

            Format requestFormat = Format.fromContentType(contentTypeValue);

            String responseStr = ParserBase.encode(oo, requestFormat);

            if (Boolean.parseBoolean(logEvent)) {
                logChannelEvent(request, rawRequest, responseStr, contentTypeValue);
            }

            request.resp.setContentType(contentTypeValue);
            request.resp.getOutputStream().print(responseStr);
            long faultCount = oo.getIssue().stream().filter(s -> OperationOutcome.IssueSeverity.FATAL.equals( s.getSeverity()) || OperationOutcome.IssueSeverity.ERROR.equals(s.getSeverity())).count();
            if (faultCount == 0) {
                request.ok();
            } else {
                logger.info(() -> String.format("IG Validation request has %d OperationOutcome Issues. URI=%s.", faultCount, request.uri));
                /*
                https://hl7.org/fhir/http.html#Status-Codes
                The OperationOutcome may be returned with any HTTP 4xx or 5xx response, but this is not required - many of these errors may be generated by generic server frameworks underlying a FHIR server.
                 */
                request.resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Use the same HTTP response status code as HAPI FHIR Validator when it catches errors
            }
        } catch (Exception ex) {
            request.serverError(ex);
        }
    }

    private static void logChannelEvent(Request request, String requestTxt, String responseTxt, String contentType) {
        try {
            SimId simId = ChannelUrl.getSimId(request.uri);
            if (!simId.isValid()) {
                String warning = ValidateFhirResourceRequest.class.getName() + " Invalid SimId in the request URI: " + request.uri;
                logger.warning(warning);
                return;
            }
            SimStore simStore;
            simStore = new SimStore(request.externalCache, simId);
            if (!simStore.exists()) {
                String warning = ValidateFhirResourceRequest.class.getName() + " simStore does no exist: " + request.uri;
                logger.warning(warning);
                return;
            }
            simStore.open();
            simStore.setResource(request.uriParts.get(4));
            Event event = simStore.newEvent();
            ITask clientTask = event.getClientTask();
            clientTask.putDescription("POST");
            Headers inHeaders = Common.getRequestHeaders(request.req, Verb.POST);
            clientTask.putRequestHeader(inHeaders);
            clientTask.putRequestBody(requestTxt.getBytes());

            if (clientTask.getEvent() != null) {
                String hostPort = ProxyServlet.getHostPort(inHeaders);
                Header h = ProxyServlet.buildEventHeader(hostPort, clientTask.getEvent().getEventDir(), Headers.X_FTK_VALIDATION_EVENT);
                Headers responseHeaders = new Headers();
                responseHeaders.add(new Header("content-type", contentType));
                responseHeaders.add(h);
                ProxyServlet.addEventHeader(request.resp, h);
                clientTask.putResponseHeader(responseHeaders);
                clientTask.putResponseBody(responseTxt.getBytes());
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "logChannelEvent failed", ex);
        }

    }


}
