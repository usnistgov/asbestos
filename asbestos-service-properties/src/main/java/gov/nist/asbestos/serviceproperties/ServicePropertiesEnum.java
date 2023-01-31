package gov.nist.asbestos.serviceproperties;

import java.util.Optional;

public enum ServicePropertiesEnum {
    MHD_CAPABILITY_STATEMENT_FILE("mhdChannelCapabilityStatementFile"),
    EMPTY_CAPABILITY_STATEMENT_FILE("emptyCapabilityStatementFile"),
    XDS_TOOLKIT_BASE("xdsToolkitBase"),
    TLS_XDS_TOOLKIT_BASE("tlsXdsToolkitBase"),
    FHIR_TOOLKIT_UI_HOME_PAGE("fhirToolkitUIHomePage"),
    HTTPS_FHIR_TOOLKIT_BASE("httpsFhirToolkitBase"),
    FHIR_TOOLKIT_BASE("fhirToolkitBase"),
    FHIR_TOOLKIT_VERSION("fhirToolkitVersion"),
    HAPI_FHIR_BASE("hapiFhirBase"),
    CAT_EXTERNAL_PATIENT_SERVER_FHIR_BASE("patientServerBase"),
//    LIMITED_CHANNEL_CAPABILITY_STATMENT_FILE("limitedChannelCapabilityStatementFile"),
//    XDS_CHANNEL_CAPABILITY_STATEMENT_FILE("xdsChannelCapabilityStatementFile"),
    FHIR_VALIDATION_SERVER("fhirValidationServer"),
    FHIR_VALIDATION_CHANNEL_ID ("fhirValidationChannelId"),
    STARTUP_SESSION_ID("startUpSession");

    private String key;

    ServicePropertiesEnum(String key) {
        this.key = key;
    }

    static public Optional<ServicePropertiesEnum> find(String s) {
        ServicePropertiesEnum found = null;
        for (ServicePropertiesEnum p : values()) {
            if (s.equals(p.key)) found = p;
        }
        return Optional.ofNullable(found);
    }

    @Override
    public String toString() {
        return key;
    }

    public boolean equals(ServicePropertiesEnum p) {
        return (p.toString().equals(this.toString()));
    }

    public boolean equals(String s) {
        return (this.toString().equals(s));
    }

    public String getKey() { return key; }

}
