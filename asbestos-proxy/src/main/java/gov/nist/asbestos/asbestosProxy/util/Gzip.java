package gov.nist.asbestos.asbestosProxy.util;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class Gzip {

    public static void decompressGZIP(File input, File output)  {
        try {
            try (GzipCompressorInputStream in = new GzipCompressorInputStream(new FileInputStream(input))) {
                IOUtils.copy(in, new FileOutputStream(output));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decompressGZIP(byte[] input)  {
        try {
            try (GzipCompressorInputStream in = new GzipCompressorInputStream(new ByteArrayInputStream(input))) {
                return new String(IOUtils.toByteArray(in));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
