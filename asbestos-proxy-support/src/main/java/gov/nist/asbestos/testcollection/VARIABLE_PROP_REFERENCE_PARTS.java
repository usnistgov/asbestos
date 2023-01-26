package gov.nist.asbestos.testcollection;

import java.util.Arrays;
import java.util.Optional;

/**
 * These are positional indexes.
 * Values must be presented in the same order.
 * Example: If the second position (0-based index) is optional in the following string, then optional values must be presented like this
 * val1:val2::val4
 * Example 2: If all but the last element or the 4th element is optional, it may be omitted
 * val1:val2:val3
 */
public enum VARIABLE_PROP_REFERENCE_PARTS {
    File(true),
    Property(true),
    DefaultToGlobalServiceProperty(false, "");

    private boolean required;
    private String token;

    VARIABLE_PROP_REFERENCE_PARTS(boolean required) {
        this.required = required;
    }

    VARIABLE_PROP_REFERENCE_PARTS(boolean required, String token) {
        this.required = required;
        this.token = token;
    }

    public boolean isRequired() {
        return required;
    }

    public String getToken() {
        return token;
    }

    /**
     * Returns count of minimum required values ignoring the last optional element
     * @return
     */
    public static int minimumExpectedLength() {
        final int count = VARIABLE_PROP_REFERENCE_PARTS.values().length;
        if (VARIABLE_PROP_REFERENCE_PARTS.values().length > 1) {
            Optional<VARIABLE_PROP_REFERENCE_PARTS> lastEnum = Arrays.asList(VARIABLE_PROP_REFERENCE_PARTS.values()).stream().reduce((first, second) -> second);
            if (lastEnum.isPresent()) {
                if (! lastEnum.get().isRequired()) {
                    return count -1;
                }
            }
        }
        return count;
    }
}
