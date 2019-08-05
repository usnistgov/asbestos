package gov.nist.asbestos.client.client;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;

import java.util.ArrayList;
import java.util.List;

public enum Format {
    JSON,
    XML,
    NONE;

    public String getContentType() {
        return this.name().equals("JSON") ? "application/fhir+json" : "application/fhir+xml";
    }

    private static List<String> formats = new ArrayList<>();

    static {
        formats.add("application/fhir+json");
        formats.add("application/fhir+xml");
    }

    public static boolean isFormat(String format) {
        return format != null && formats.contains(format);
    }

    public static Format fromContentType(String contentType) {
        if (contentType != null && contentType.contains("json"))
            return Format.JSON;
        return Format.XML;
    }

    public static Format resultContentType(Headers inHeaders) {
        Header acceptHeader = inHeaders.getAccept();
        Format format = Format.XML;
        if (acceptHeader != null && acceptHeader.getValue().contains("json"))
            format = Format.JSON;
        return format;
    }
}
