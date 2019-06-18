package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.mhd.exceptions.MetadataAttributeTranslationException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTransform {
    private static String dtmPattern = "yyyyMMddHHmmss";
    private static String fhirPattern = "yyyy-MM-dd'T'HH:mm:ss";
    private static SimpleDateFormat dtmFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat fhirFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static Date dtmToDate(String dtm) throws MetadataAttributeTranslationException {
        try {
            return dtmFormat.parse(dtm);
        } catch (Throwable t) {
            throw new MetadataAttributeTranslationException("Cannot translate date " + dtm, t);
        }
    }

    static String fhirToDtm(Date date) {
        return dtmFormat.format(date);
    }

    static public Date xdsPrecision(Date date) throws MetadataAttributeTranslationException {
        return dtmToDate(fhirToDtm(date));
    }

}
