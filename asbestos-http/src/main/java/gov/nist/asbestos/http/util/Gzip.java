package gov.nist.asbestos.http.util;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
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

    public static String decompressGZIPToString(byte[] input)  {
        try {
            try (GzipCompressorInputStream in = new GzipCompressorInputStream(new ByteArrayInputStream(input))) {
                return new String(IOUtils.toByteArray(in));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decompressGZIP(byte[] input)  {
        try {
            try (GzipCompressorInputStream in = new GzipCompressorInputStream(new ByteArrayInputStream(input))) {
                return IOUtils.toByteArray(in);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] compressGZIP(byte[] input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            GzipCompressorOutputStream cos = null;
            try {
                cos = new GzipCompressorOutputStream(baos);
                cos.write(input);
                cos.flush();

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cos != null)
                    cos.close();
                baos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

}
