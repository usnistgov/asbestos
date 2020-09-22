package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;

import java.util.Map;

public class EventAnalysisParams {
    private boolean gzip;
    private boolean useProxy;
    private boolean ignoreBadRefs;
    private boolean validation;

    EventAnalysisParams(Request request) {
        Map<String, String> queryParams = request.getParametersMap();
        gzip = queryParams.containsKey("gzip") && "true".equals(queryParams.get("gzip"));
        useProxy = queryParams.containsKey("useProxy") && "true".equals(queryParams.get("useProxy"));
        ignoreBadRefs = queryParams.containsKey("ignoreBadRefs") && "true".equals(queryParams.get("ignoreBadRefs"));
        validation = queryParams.containsKey("validation") && "true".equals(queryParams.get("validation"));
    }

    public boolean isGzip() {
        return gzip;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public boolean isIgnoreBadRefs() {
        return ignoreBadRefs;
    }

    public boolean isValidation() {
        return validation;
    }
}
