package gov.nist.asbestos.client.Base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class Returns {

    // This doesn't work if the values are JSON
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

    public static void returnString(HttpServletResponse resp, String json) {
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class ValueHolder {
        String value;
        ValueHolder(String value) {
            this.value = value;
        }
    }

    static String returnValue(HttpServletResponse resp, String value) {
           return returnObject(resp, new ValueHolder(value));
    }

    public static String returnObject(HttpServletResponse resp, Object o) {
        String json = new Gson().toJson(o);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().write(json.getBytes());
            return json;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void returnOperationOutcome(HttpServletResponse resp, OperationOutcome.IssueSeverity severity, OperationOutcome.IssueType issueType, String diagnostics) {
        OperationOutcome oo = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent comp = oo.addIssue();
        comp.setSeverity(severity);
        comp.setDiagnostics(diagnostics);
        comp.setCode(issueType);
        returnObject(resp, oo);
    }
}
