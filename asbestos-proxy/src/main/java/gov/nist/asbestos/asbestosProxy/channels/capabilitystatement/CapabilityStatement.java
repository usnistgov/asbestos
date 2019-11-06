package gov.nist.asbestos.asbestosProxy.channels.capabilitystatement;

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Objects;

public class CapabilityStatement {
    private static Logger logger = Logger.getLogger(CapabilityStatement.class);

    /**
     *
     * @param baseUri Only used for counting path segment parts purposes. Does not look at the actual segment values.
     * @param requestUri The actual GET Request header path.
     * @return
     */
    public static boolean isCapabilityStatementRequest(URI baseUri, URI requestUri) {
        Objects.requireNonNull(baseUri);
        Objects.requireNonNull(requestUri);
        try {
            // Ignoring the query string, exclusively check for the only segment right after the base path.
            // Should match: base-path/segment
            // But not: base-path/abc/segment/sdf or base-path/segment/abc
            String[] basePath = baseUri.getPath().split("/");
            if (basePath.length > 0) {
                String[] requestPath = requestUri.getPath().split("/");
                if (requestPath.length == basePath.length + 1 /* 1=the last segment that we are interested in */) {
                    if ("metadata".equals(requestPath[basePath.length])) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            logger.info("isCapabilityStatementRequest: " + ex.toString());
        }
        return false;
    }

    public static BaseResource getCapabilityStatement(ServicePropertiesEnum key) throws Exception {
        Objects.requireNonNull(key);
        String capabilityStatementFileName =
                ServiceProperties.getInstance().getProperty(key);
//                "capabilitystatement/capabilitystatement-fhirToolkitDocRecipientDocResponder.xml";
//                  "capabilitystatement/empty-capabilitystatement-base2.xml";

        File capabilityStatementFile = Paths.get(CapabilityStatement.class.getResource("/").toURI()).resolve(capabilityStatementFileName).toFile();

        if (capabilityStatementFile.exists()) {
            // TODO: replace any ${} parameters in the File stream such as the ${ProxyBase}

            // Comments in XML are also parsed as part of the BaseResource. As noticed in the JSON Format, XML begin/end comments are not necessarily meaningful when it gets parsed
            BaseResource baseResource = ProxyBase.parse(capabilityStatementFile);
           return baseResource;
        }
        throw new RuntimeException(String.format("Error: File '%s' was not found.", capabilityStatementFile.toString()));
    }

}
