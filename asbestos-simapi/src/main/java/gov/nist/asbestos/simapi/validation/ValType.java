package gov.nist.asbestos.simapi.validation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ValType {
    Msg("Msg"),
    Error("Error"),
    Warn("Warn"),
    Ref("Reference"),
    Doc("Documentation"),
    Ignored("Ignored"),
    Translation("Translation"),
    IHERequirement("IHE Requirement"),
    Reference("Reference");
private String title;
    ValType(final String title) {
        this.title = title;
    }

    public String toString() {
        return title;
    }

    @JsonValue
    final String title() {
        return this.title;
    }
};
