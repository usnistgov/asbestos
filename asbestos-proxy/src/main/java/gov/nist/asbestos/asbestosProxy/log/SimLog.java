package gov.nist.asbestos.asbestosProxy.log;


import java.io.File;

public class SimLog {
    private File root;

    public SimLog(File externalCache) {
        root = new File(externalCache, "");
    }
}
