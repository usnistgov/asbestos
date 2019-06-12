package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.mhd.exceptions.MetadataAttributeTranslationException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTransform {
    private static String dtmPattern = "yyyyMMddHHmmss";
    private static String fhirPattern = "yyyy-MM-dd'T'HH:mm:ss";
    private static SimpleDateFormat dtmFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat fhirFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static String dtmToFhir(String dtm) throws MetadataAttributeTranslationException {
        int dsize = dtm.length();
        Date date = dtmToDate(dtm);
        int fsize = fsizeFromDsize(dsize);
        return fhirFormat.format(date).substring(0, fsize);
    }

    static Date dtmToDate(String dtm) throws MetadataAttributeTranslationException {
        int dsize = dtm.length();
        String pattern = dtmPattern.substring(0, dsize);
        SimpleDateFormat dtmFormat = new SimpleDateFormat(pattern);
        try {
            return dtmFormat.parse(dtm);
        } catch (Throwable t) {
            throw new MetadataAttributeTranslationException("Cannot translate date " + dtm, t);
        }
    }

    static String fhirToDtm(String fhir) throws MetadataAttributeTranslationException {
        int fsize = (fhir.length() > 10) ? fhir.length() + 2 : fhir.length();
        String pattern = fhirPattern.substring(0, fsize);
        SimpleDateFormat fhirFormat = new SimpleDateFormat(pattern);
        Date date;
        try {
            date = fhirFormat.parse(fhir);
        } catch (Throwable t) {
            throw new MetadataAttributeTranslationException("Cannot translate date " + fhir, t);
        }
        int dsize = dsizeFromFsize(fsize);
        return dtmFormat.format(date).substring(0, dsize);
    }

    private static int dsizeMax = dtmPattern.length();
    static int dsizeFromFsize(int fsize) {
        switch (fsize) {
            case 4 : return 4;
            case 7: return 6;
            case 10: return 8;
            case 16: return 10;
            case 18: return 12;
            default: return dsizeMax;
        }
    }

    private static int fsizeMax = "yyyy-mm-ddThh:mm:ss".length();
    private static int fsizeFromDsize(int dsize) {
        switch (dsize) {
            case 4: return 4;
            case 6: return 7;
            case 8: return 10;
            case 12: return 16;
            default: return fsizeMax;
        }
    }

}
