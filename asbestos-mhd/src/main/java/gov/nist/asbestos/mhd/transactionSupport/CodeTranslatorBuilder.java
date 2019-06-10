package gov.nist.asbestos.mhd.transactionSupport;

import java.io.File;
import java.io.InputStream;

public class CodeTranslatorBuilder {

    public static CodeTranslator read(InputStream is) {
        try {
            return new CodeTranslator(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CodeTranslator read(File file) {
        try {
            return new CodeTranslator(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
