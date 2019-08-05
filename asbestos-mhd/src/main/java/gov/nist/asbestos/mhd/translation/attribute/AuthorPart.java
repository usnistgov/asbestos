package gov.nist.asbestos.mhd.translation.attribute;

import gov.nist.asbestos.simapi.validation.Val;

import java.util.Objects;

public abstract class AuthorPart {
    String value;
    String[] parts;

    abstract void validate(Val val);

    public void parse() {
        parts = value.split("\\^");
    }

    public AuthorPart setValue(String value, Val val) {
        Objects.requireNonNull(val);
        this.value = value;
        parse();
        validate(val);
        return this;
    }

    public String get(int i) {
        if (i > parts.length)
            return "";
        return parts[i-1];
    }
}
