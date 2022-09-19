package gov.nist.asbestos.client.Base;

import ca.uhn.fhir.context.FhirContext;
import gov.nist.asbestos.client.client.Format;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class ParserBase {
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
        return parse(fileContent, getFormat(resourceFile));
    }

    public static Format getFormat(File resourceFile) {
        return resourceFile.toString().endsWith("json") ? Format.JSON : Format.XML;
    }

    public static Bundle bundleWith(List<Resource> in) {
        Bundle bundle = new Bundle();

        for (Resource resource : in) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            bundle.addEntry(entry);
            if (resource instanceof OperationOutcome) {
                Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
                resp.setStatus("500");
                resp.setOutcome((OperationOutcome)resource);
            } else {
                entry.setResource(resource);
                Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
                entry.setResponse(resp);
                resp.setStatus("200");
            }
        }

        return bundle;
    }

}
