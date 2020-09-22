package gov.nist.asbestos.client.log;


import java.io.File;

public class SimLog {
    private File root;

    public SimLog(File externalCache) {
        root = new File(externalCache, "");
    }
}
