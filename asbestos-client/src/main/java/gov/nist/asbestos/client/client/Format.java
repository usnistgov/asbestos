package gov.nist.asbestos.client.client;

public enum Format {
    JSON,
    XML,
    NONE;

    public String getContentType() {
        return this.name().equals("JSON") ? "application/fhir+json" : "application/fhir+xml";
    }
}
