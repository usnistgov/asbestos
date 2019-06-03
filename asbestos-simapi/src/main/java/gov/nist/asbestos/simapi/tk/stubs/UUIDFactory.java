package gov.nist.asbestos.simapi.tk.stubs;

import java.util.UUID;

public class UUIDFactory {
    public static UUIDFactory getInstance() { return new UUIDFactory(); }

    public UUID newUUID() {
        return UUID.randomUUID();
    }
}
