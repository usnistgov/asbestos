package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.BaseResource;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class Returns {

    static void returnList(HttpServletResponse resp, List<String> values) {
        String json = new Gson().toJson(values);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String returnResource(HttpServletResponse resp, BaseResource resource) {
        String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        returnString(resp, json);
        return json;
    }

    static void returnString(HttpServletResponse resp, String json) {
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
