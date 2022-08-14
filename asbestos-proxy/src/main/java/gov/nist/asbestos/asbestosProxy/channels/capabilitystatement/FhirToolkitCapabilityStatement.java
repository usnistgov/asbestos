package gov.nist.asbestos.asbestosProxy.channels.capabilitystatement;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.log.SimStoreFactory;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdCanonicalUriCodeInterface;
import gov.nist.asbestos.mhd.channel.MhdVersionEnum;
import gov.nist.asbestos.mhd.channel.UriCodeTypeEnum;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class FhirToolkitCapabilityStatement {
    public static final String XDS_LIMITED_META_SIM = "default__asbtslimited";
    public static final String XDS_COMPREHENSIVE_META_SIM = "default__asbtsrr";

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
            logger.severe("isCapabilityStatementRequest: " + ex.toString());
        }
        return false;
    }

    public static CapabilityStatement getCapabilityStatement(ServicePropertiesEnum key, ChannelConfig channelConfig) throws Exception {
        Objects.requireNonNull(key);
        Objects.requireNonNull(channelConfig);

        String channelId = channelConfig.getChannelName();
        String capabilityStatementFileName = ServiceProperties.getInstance().getPropertyOrThrow(key);

        File capabilityStatementFile = Paths.get(FhirToolkitCapabilityStatement.class.getResource("/").toURI()).resolve(capabilityStatementFileName).toFile();

        if (capabilityStatementFile != null && capabilityStatementFile.exists()) {
            // Replace any ${} parameters in the File stream such as the ${ProxyBase}
            String statementContent = new String(Files.readAllBytes(capabilityStatementFile.toPath()));
            /*
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
             */

            // Comments in XML are also parsed as part of the BaseResource. As noticed in the JSON Format, XML begin/end comments are not necessarily meaningful when it gets parsed
            Format format = ParserBase.getFormat(capabilityStatementFile);
            BaseResource baseResource = ParserBase.parse(statementContent, format);
            if (baseResource instanceof CapabilityStatement) {
                CapabilityStatement capabilityStatement = (CapabilityStatement) baseResource;
                try {
                    List<CapabilityStatement.SystemInteractionComponent> interactionComponents = getChannelInteractions(channelConfig.getXdsSiteName(), channelConfig.getMhdVersions());
                    CapabilityStatement.CapabilityStatementRestComponent restComponent = new CapabilityStatement.CapabilityStatementRestComponent();
                    restComponent.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
                    restComponent.setInteraction(interactionComponents);
                    capabilityStatement.addRest(restComponent);
                } catch (Exception ex) {
                    logger.warning(ex.toString());
                } finally {
                    return capabilityStatement;
                }
            } else {
                throw new RuntimeException("File does not contain a CapabilityStatement resource.");
            }

        }
        throw new RuntimeException(String.format("Error: File '%s' was not found.", capabilityStatementFile.toString()));
    }

    private static List<CapabilityStatement.SystemInteractionComponent> getChannelInteractions(String xdsSitename, String[] mhdVersions ) throws Exception {
        if (mhdVersions == null || (mhdVersions != null && mhdVersions.length == 0)) {
            throw new Exception("channelConfig mhdVersion must be specified.");
        }
        List<CapabilityStatement.SystemInteractionComponent> systemInteractionComponents = new ArrayList<>();
        for (String s : mhdVersions) {
            Class<? extends MhdCanonicalUriCodeInterface> myUriCodesClass = MhdVersionEnum.find(s).getUriCodesClass();
            MhdCanonicalUriCodeInterface intf = myUriCodesClass.getDeclaredConstructor().newInstance();

            String doc;
            switch (xdsSitename) {
                case XDS_LIMITED_META_SIM: doc = intf.getUriCodesByType(UriCodeTypeEnum.PROFILE).get(CanonicalUriCodeEnum.MINIMAL); break;
                case XDS_COMPREHENSIVE_META_SIM: doc = intf.getUriCodesByType(UriCodeTypeEnum.PROFILE).get(CanonicalUriCodeEnum.COMPREHENSIVE); break;
                default: throw new Exception("Unrecognized xdsSiteName: " + xdsSitename);
            }

            systemInteractionComponents.add( new CapabilityStatement.SystemInteractionComponent()
                    .setCode(CapabilityStatement.SystemRestfulInteraction.TRANSACTION)
                    .setDocumentation(doc));
        }
       return systemInteractionComponents;
    }

}
