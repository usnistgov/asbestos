package gov.nist.asbestos.asbestosApi.impl;

import java.util.Objects;

public class ApiConfig {
    private static String asbestosBase;

    static public void setAsbestosBase(String asbestosBase1) {
        Objects.requireNonNull(asbestosBase1);
        asbestosBase = asbestosBase1;
    }

    static public String getAsbestosBase() {
        Objects.requireNonNull(asbestosBase);
        return asbestosBase;
    }
}
