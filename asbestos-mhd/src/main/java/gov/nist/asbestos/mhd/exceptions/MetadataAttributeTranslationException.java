package gov.nist.asbestos.mhd.exceptions;

public class MetadataAttributeTranslationException extends Exception {
    public MetadataAttributeTranslationException(String msg) {
        super(msg);
    }

    public MetadataAttributeTranslationException(String msg, Throwable t) {
        super(msg, t);
    }
}
