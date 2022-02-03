package gov.nist.asbestos.client.Base;

import com.google.gson.Gson;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

    static void returnJson(HttpServletResponse resp, File file) {
        String json = EC.readFromFile(file);
        returnString(resp, json);
    }

    static String returnResource(HttpServletResponse resp, BaseResource resource) {
        String json = ParserBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        returnString(resp, json);
        return json;
    }

    public static void returnString(HttpServletResponse resp, String json) {
        resp.setContentType("application/json");
        try {
            resp.setCharacterEncoding("UTF-8"); // now equivalent to setContentType "application/json; charset=UTF-8"
            PrintWriter writer = resp.getWriter();
            writer.print(json);
            // Binary output
//            resp.getOutputStream().print(json);
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

    public static void returnPlainTextResponse(ServletResponse response, int httpStatusCode, String message) throws IOException {
        response.resetBuffer();
        ((HttpServletResponse) response).setStatus(httpStatusCode);
        response.setContentType("text/plain");
        response.getOutputStream().print(message);
        response.flushBuffer();
    }

}
