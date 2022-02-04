package gov.nist.asbestos.asbestosProxy.channels.capabilitystatement;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import java.util.logging.Logger;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class FhirToolkitCapabilityStatement {
    private static Logger logger = Logger.getLogger(FhirToolkitCapabilityStatement.class.getName());

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

    public static BaseResource getCapabilityStatement(ServicePropertiesEnum key, String channelId) throws Exception {
        Objects.requireNonNull(key);
        String capabilityStatementFileName = ServiceProperties.getInstance().getPropertyOrThrow(key);

        File capabilityStatementFile = Paths.get(FhirToolkitCapabilityStatement.class.getResource("/").toURI()).resolve(capabilityStatementFileName).toFile();

        if (capabilityStatementFile != null && capabilityStatementFile.exists()) {
            // Replace any ${} parameters in the File stream such as the ${ProxyBase}
            String statementContent = new String(Files.readAllBytes(capabilityStatementFile.toPath()));
            statementContent = statementContent.replaceAll(Pattern.quote("${channelId}"), channelId);
            for (ServicePropertiesEnum paramKey: ServicePropertiesEnum.values()) {
                    String param = String.format("${%s}", paramKey.getKey());
                    if (statementContent.contains(param)) {
                        Optional<String> paramValue = ServiceProperties.getInstance().getProperty(paramKey);
                        if (paramValue.isPresent()) {
                            statementContent = statementContent.replaceAll(Pattern.quote(param),  paramValue.get());
                        } else {
                            logger.warning("No service property value found for key: " + paramKey);
                        }
                    }
                }

            // Comments in XML are also parsed as part of the BaseResource. As noticed in the JSON Format, XML begin/end comments are not necessarily meaningful when it gets parsed
            Format format = ParserBase.getFormat(capabilityStatementFile);
            BaseResource baseResource = ParserBase.parse(statementContent, format);
           return baseResource;
        }
        throw new RuntimeException(String.format("Error: File '%s' was not found.", capabilityStatementFile.toString()));
    }

}
