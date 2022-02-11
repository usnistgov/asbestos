package gov.nist.asbestos.services.restRequests;


// TODO - this request seems not to follow any of the rules.  Does it work?

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "validate"
// 3 - eventId
// 4 - resourceType
// validates response object from event

// returns OperationOutcome


import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import java.util.logging.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

public class GetValidationRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "Validate".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetValidationRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() {
        request.announce("GetValidationRequest");
        String resourceType = request.uriParts.get(3);
        String base = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        request.returnValue(base);

        String query = request.req.getQueryString();
        if (query == null) {
            Returns.returnOperationOutcome(request.resp,
                    OperationOutcome.IssueSeverity.FATAL,
                    OperationOutcome.IssueType.INCOMPLETE,
                    "query not present on request to asbestos server");
            return;
        }
        boolean gzip = false;
        boolean useProxy = false;
        Ref ref = null;
        if (query.contains("gzip=true"))
            gzip = true;
        if (query.contains("useProxy=true"))
            useProxy = true;
        if (query.contains("url=http")) {
            int urlIndex = query.indexOf("url=http") + 4;
            int urlEndIndex = query.indexOf(";", urlIndex);
            String url = query.substring(urlIndex, urlEndIndex);
            ref = new Ref(url);
        }
        if (ref == null) {
            Returns.returnOperationOutcome(request.resp,
                    OperationOutcome.IssueSeverity.FATAL,
                    OperationOutcome.IssueType.INCOMPLETE,
                    "url parameter not present in request to asbestos server");
            return;
        }

        ResourceWrapper wrapper1 = new FhirClient().readResource(ref);
        BaseResource resource = wrapper1.getResponseResource();
        if (resource == null) {
            Returns.returnOperationOutcome(request.resp,
                    OperationOutcome.IssueSeverity.FATAL,
                    OperationOutcome.IssueType.INCOMPLETE,
                    "url parameter references resource that cannot be accessed by asbestos server");
            return;
        }
        String validationServer = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        FhirClient fhirClient = new FhirClient()
                .sendGzip(gzip)
                .requestGzip(gzip);
        ResourceWrapper wrapper = fhirClient.writeResource(resource,
                new Ref(validationServer + "/" + resourceType + "/$validate?profile=http://hl7.org/fhir/StructureDefinition/" + resourceType),
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()));
        if (wrapper.getResponseResource() == null) {
            Returns.returnOperationOutcome(request.resp,
                    OperationOutcome.IssueSeverity.FATAL,
                    OperationOutcome.IssueType.INCOMPLETE,
                    "request to validation server (" +  validationServer  + " ) failed");
            return;
        }
        if ("OperationOutcome".equals(wrapper.getResponseResource().getClass().getSimpleName())) {
            request.returnResource(wrapper.getResponseResource());
            return;
        }
        Returns.returnOperationOutcome(request.resp,
                OperationOutcome.IssueSeverity.FATAL,
                OperationOutcome.IssueType.INCOMPLETE,
                "OperationOutcome not returned from  validation server (" +  validationServer  + " ) "
        + wrapper.getResponseResource().getClass() + " returned instead");
    }
}
