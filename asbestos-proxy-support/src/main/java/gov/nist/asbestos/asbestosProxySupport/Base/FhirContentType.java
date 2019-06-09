package gov.nist.asbestos.asbestosProxySupport.Base;

enum FhirContentType {
    JSON("application/fhir+json", true), XML("application/fhir+xml", false);

    String contentType;
    boolean isJson;

    private FhirContentType(String type, boolean json) {
        contentType = type;
        isJson = json;
    }
}
