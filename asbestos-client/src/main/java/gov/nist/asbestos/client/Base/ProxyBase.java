package gov.nist.asbestos.client.Base;

import ca.uhn.fhir.context.FhirContext;
import gov.nist.asbestos.client.client.Format;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ProxyBase {
    static private FhirContext ourCtx;

    public static FhirContext getFhirContext() {
        if (ourCtx == null)
            ourCtx = FhirContext.forR4();
        return ourCtx;
    }

    public static String encode(BaseResource resource, Format format) {
        if (format == Format.JSON) {
            return getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        } else {
            return getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
        }
    }

    public static void toFile(BaseResource resource, File dir, String id, Format format) {
        String content = encode(resource, format);
        Path path = new File(dir, id + "." + Format.fileExtensionFromContent(content)).toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static BaseResource parse(byte[] resource, Format format) {
        String resourceString = new String(resource);
        return parse(resourceString, format);
    }

    public static BaseResource parse(String resourceString, Format format) {
        IBaseResource ibase;
        if (format == Format.JSON) {
            ibase = getFhirContext().newJsonParser().parseResource(resourceString);
            if (ibase instanceof BaseResource)
                return (BaseResource) ibase;
        } else {
            ibase = getFhirContext().newXmlParser().parseResource(resourceString);
            if (ibase instanceof BaseResource)
                return (BaseResource) ibase;
        }
        throw new RuntimeException("Cannot parse resource - type " + ibase.getClass().getSimpleName() + " cannot be converted to BaseResource");
    }

    public static BaseResource parse(File resourceFile) {
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(resourceFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + resourceFile.toString(), e);
        }
        return parse(fileContent, resourceFile.toString().endsWith("json") ? Format.JSON : Format.XML);
    }

//    private static hexChars = ('0'..'9') + ('a'..'f')
//    static boolean isUUID(String u) {
//        if (u.startsWith('urn:uuid:')) return true
//        try {
//            int total = 0
//
//            total += (0..7).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[8] == '-') ? 0 : 1
//            total += (9..12).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[13] == '-') ? 0 : 1
//            total += (14..17).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[18] == '-') ? 0 : 1
//            total += (19..22).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[23] == '-') ? 0 : 1
//            total += (24..35).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            return total == 0
//        } catch (Exception e) {
//            return false
//        }
//    }
}
