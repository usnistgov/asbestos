package gov.nist.asbestos.serviceproperties;

import java.util.Optional;

public enum ServicePropertiesEnum {
    MHD_CAPABILITY_STATEMENT_FILE("mhdCapabilityStatementFile"),
    EMPTY_CAPABILITY_STATEMENT_FILE("emptyCapabilityStatementFile"),
    XDS_TOOLKIT_BASE("xdsToolkitBase"),
    TLS_XDS_TOOLKIT_BASE("tlsXdsToolkitBase"),
    TLS_UI_FHIR_TOOLKIT_BASE("httpsFhirToolkitUIBase"),
    UI_FHIR_TOOLKIT_BASE("httpFhirToolkitUIBase"),
    FHIR_TOOLKIT_BASE("fhirToolkitBase"),
    FHIR_TOOLKIT_VERSION("fhirToolkitVersion"),
    HAPI_FHIR_BASE("hapiFhirBase"),
    LIMITED_CHANNEL_CAPABILITY_STATMENT_FILE("limitedChannelCapabilityStatementFile"),
    XDS_CHANNEL_CAPABILITY_STATEMENT_FILE("xdsChannelCapabilityStatementFile"),
    FHIR_VALIDATION_SERVER("fhirValidationServer");

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
