package gov.nist.asbestos.simapi.tk.installation;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class Initialization {

    public static void initTypes(File ec) throws Exception {
        if (!(ec.exists() && ec.isDirectory() && ec.canWrite()))
            throw new Exception("External Cache " + ec + " must exist, be a directory, and be writable");

        File registryDef = new File(Initialization.class.getResource("/types/actors/reg.json").toURI());
        if (!registryDef.exists())
            throw new Exception("Cannot find definition of Registry actor in /types/actors/reg.json in JAR file");
        File types = registryDef.getParentFile().getParentFile();
        if (types == null)
            throw new Exception("Cannot find types resource in Initialization constructor");

        File internalTransactionTypesDir = new File(types, "transactions");
        if (!internalTransactionTypesDir.exists())
            throw new Exception("No internal TransactionTypes");
        File internalActorTypesDir = new File(types, "actors");
        if (!internalActorTypesDir.exists())
            throw new Exception("No internal ActorTypes");

        FileUtils.copyDirectoryToDirectory(internalTransactionTypesDir, typesFile(ec));

        FileUtils.copyDirectoryToDirectory(internalActorTypesDir, typesFile(ec));

    }

    private static File typesFile(File ec) {
       return new File(ec, "types");
    }
}
