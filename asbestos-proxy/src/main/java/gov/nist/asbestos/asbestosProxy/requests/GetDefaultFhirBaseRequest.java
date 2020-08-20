package gov.nist.asbestos.asbestosProxy.requests;


// 0 - empty
// 1 - app context
// 2 - "hapiFhirBase"

// returns JSON object with "fhirBase": "fhirBaseValue"


import com.google.gson.Gson;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;

import java.io.IOException;

public class GetDefaultFhirBaseRequest {
    private static Logger log = Logger.getLogger(GetDefaultFhirBaseRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 3 && "hapiFhirBase".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetDefaultFhirBaseRequest(Request request) { this.request = request; }

    static class FhirBase {
        String fhirBase;

        FhirBase(String value) {
            fhirBase = value;
        }
    }

    public void run() throws IOException {
        request.announce("GetDefaultFhirBase");

        FhirBase fhirBase = new FhirBase(ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.HAPI_FHIR_BASE).get());

        String json = new Gson().toJson(fhirBase);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);

        request.ok();
    }
}
