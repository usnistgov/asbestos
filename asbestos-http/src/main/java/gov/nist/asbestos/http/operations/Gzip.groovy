package gov.nist.asbestos.http.operations

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Gzip {

    static String zip(String s){
        def targetStream = new ByteArrayOutputStream()
        def zipStream = new GZIPOutputStream(targetStream)
        zipStream.write(s.getBytes('UTF-8'))
        zipStream.close()
        def zippedBytes = targetStream.toByteArray()
        targetStream.close()
        return zippedBytes.encodeBase64()
    }

    static String unzipWithBase64(String compressed){
        def inflaterStream = new GZIPInputStream(new ByteArrayInputStream(compressed.decodeBase64()))
        def uncompressedStr = inflaterStream.getText('UTF-8')
        return uncompressedStr
    }

    static String unzipWithoutBase64(byte[] compressed){
        def inflaterStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
        def uncompressedStr = inflaterStream.getText('UTF-8')
        return uncompressedStr
    }
}
