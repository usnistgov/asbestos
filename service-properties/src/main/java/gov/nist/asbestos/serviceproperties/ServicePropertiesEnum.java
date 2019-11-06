package gov.nist.asbestos.serviceproperties;

import java.util.Optional;

public enum ServicePropertiesEnum {
    LOG_CS_METADATA_REQUEST("Log_CS_Metadata_Request"),
    MHD_CAPABILITY_STATEMENT_FILE("MhdCapabilityStatementFile"),
    EMPTY_CAPABILITY_STATEMENT_FILE("EmptyCapabilityStatementFile");


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
