package gov.nist.asbestos.mhd.resolver;

import gov.nist.asbestos.simapi.tk.installation.Installation;

import java.util.Date;

public class IdBuilder {
    private boolean override = false;

    public IdBuilder(boolean override) {
        this.override = override;
    }

    public String allocate(String defaultValue) {
        if (!override)
            return defaultValue;
        return Installation.dateAsIdentifier(new Date(), "1.2.9760.", ".");
    }
}
