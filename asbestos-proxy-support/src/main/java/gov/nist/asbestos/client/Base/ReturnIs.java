package gov.nist.asbestos.client.Base;

import com.google.common.base.Strings;

public class ReturnIs {

    /**
     * Any boolean value is acceptable.
     * @param value
     */
    static public void Boolean(boolean value) {

    }

    /**
     * Must be true
     * @param value
     */
    static public void True(boolean value) {
        if (!value)
            throw new RuntimeException("Value must be true");
    }

    /**
     * Must be false
     * @param value
     */
    static public void False(boolean value) {
        if (value)
            throw new RuntimeException("Value must be false");
    }

    /**
     * Must be valid string
     * @param value
     */
    static public void NotNull(Object value) {
        if (value == null)
            throw new RuntimeException("String value must be valid (not null)");
    }
}
