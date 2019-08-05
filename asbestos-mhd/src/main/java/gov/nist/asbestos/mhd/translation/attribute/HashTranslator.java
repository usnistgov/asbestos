package gov.nist.asbestos.mhd.translation.attribute;

import javax.xml.bind.DatatypeConverter;

public class HashTranslator {

    public static byte[] toByteArray(String hash) {
        return DatatypeConverter.parseHexBinary(hash);
    }

    static byte[] toByteArrayFromBase64Binary(String hash) {
        return DatatypeConverter.parseBase64Binary(hash);
    }

    static String fromByteArray(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    }
}
