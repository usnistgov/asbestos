package gov.nist.asbestos.mhd.translation.search;

import java.util.*;

/**
 * generate SQ parameters from MHD query spec.  Inputs and outputs are in a Map-based model.
 */
public class DocManSQParamTranslator {
    // SubmissionSet SQ parameters
    public static final String SSuuidKey = "$XDSSubmissionSetEntryUUID";
    public static final String SSuidKey = "$XDSSubmissionSetUniqueId";
    public static final String PatientId = "$XDSSubmissionSetPatientId";
    public static final String Status = "$XDSSubmissionSetStatus";

    // query types
    public static final String GetSubmissionSetAndContentsKey = "urn:uuid:e8e3cb2c-e39c-46b9-99e4-c12f57260b83";
    private Map<String, List<String>> result = new HashMap<>();

    public DocManSQParamTranslator() {
        result.put("QueryType", Arrays.asList(GetSubmissionSetAndContentsKey));
    }

    Map<String, List<String>> run(String httpQueryString) {
        Map<String, List<String>> result = new HashMap<>();
        for (String param : parseParms(httpQueryString)) {
            Map<String, List<String>> r = runAParm(param);
            result.putAll(r);
        }
        return result;
    }

    List<String> parseParms(String parmString) {
        List<String> delimiters = Arrays.asList("&",";");
        final List<String> params = new ArrayList<>();
        delimiters.stream().forEach(s -> {
            if (params.isEmpty()) { // unparsed state
                if (parmString.contains(s)) {
                    params.addAll(Arrays.asList(parmString.split(s)));
                }
            }
        });
        if (params.isEmpty()) {// if true then no delimiters were detected in the query string
            params.add(parmString);
        }
        return params;
    }


    private void addResult(String key, String value) {
        result.put(key, Arrays.asList(value));
    }

    Map<String, List<String>> runAParm(String param) {

        String[] paramParts = param.split("=", 2);
        String name = paramParts[0];
        String value = paramParts[1];

        switch (name) {
            case "patient":
            case "patient.identifier":
                addResult(PatientId, value);
                break;
            case "status":
                addResult(Status, value);
                break;
            case "identifier":
                addResult(SSuidKey, stripUrn(value));
                break;
            case "author":
            case "created":
            case "description":
            case "item":
            case "recipient":
            case "related-id":
            case "related-ref":
            case "source":
            case "subject":
            case "type":
                throw new RuntimeException("Search on " + name + " not implemented");
        }
        return result;
    }

    String stripUrn(String val) {
        if (val == null)
            return null;
        if (val.startsWith("urn:oid:"))
            return val.substring("urn:oid:".length());
        return val;
    }
}
