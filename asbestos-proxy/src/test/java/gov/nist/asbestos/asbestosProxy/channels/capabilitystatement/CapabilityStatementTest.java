package gov.nist.asbestos.asbestosProxy.channels.capabilitystatement;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.mhd.transforms.MhdV3xCanonicalUriCodes;
import gov.nist.asbestos.mhd.transforms.MhdV410CanonicalUriCodes;
import gov.nist.asbestos.mhd.transforms.MhdV4CanonicalUriCodes;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

class CapabilityStatementTest {

    @Test
    void isCapabilityStatementRequest() throws Exception {
            URI baseUri = new URI("/asbestos/proxy/default__mhdchannel");
            List<URI> goodMetadataRequestUri = Arrays.asList(
                    new URI("/asbestos/proxy/default__mhdchannel/metadata"),
                    new URI("/asbestos/proxy/default__mhdchannel/metadata?mode=full"));
            List<URI> badMetadataRequestUri = Arrays.asList(
                    new URI("/asbestos/proxy/default__mhdchannel/metadata/something"),
                    new URI("/asbestos/proxy/default__mhdchannel/somethingelse"),
                    new URI("/asbestos/proxy/default__mhdchannel/somethingelse/metadata"));

            Predicate<URI> uriPredicate = s -> FhirToolkitCapabilityStatement.isCapabilityStatementRequest(baseUri, s);

            goodMetadataRequestUri.forEach(s -> {assert uriPredicate.test(s);});
            badMetadataRequestUri.forEach(s -> {assert uriPredicate.negate().test(s);});
    }

    @Test
    void testMhd3xNoMhdVersCompCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_COMPREHENSIVE_META_SIM);
        channelConfig.setCcFhirIgName(null);
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("limited");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert !capabilityStatement.hasRest();
    }
    @Test
    void testMhd3xCompCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv3x.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_COMPREHENSIVE_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("xds");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        String profile = new MhdV3xCanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
        assert profile.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }
    @Test
    void testMhd3xMinCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv3x.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_LIMITED_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("limited");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        {
            String profile = new MhdV3xCanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.MINIMAL);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        }
        {
            String profile = new MhdV3xCanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(1).getDocumentation());
        }
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }
    @Test
    void testMhd4MinCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv4.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_LIMITED_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("v4limited");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        {
            String profile = new MhdV4CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.MINIMAL);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        }
        {
            String profile = new MhdV4CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(1).getDocumentation());
        }
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }
    @Test
    void testMhd4CompCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv4.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_COMPREHENSIVE_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("v4xds");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        String profile = new MhdV4CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
        assert profile.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }
    @Test
    void testMhd410MinCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv410.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_LIMITED_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("v410limited");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        {
            String profile = new MhdV410CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.MINIMAL);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        }
        {
            String profile = new MhdV410CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
            assert profile.equals(capabilityStatement.getRest().get(0).getInteraction().get(1).getDocumentation());
        }
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }
    @Test
    void testMhd410CompCapabilityStatement() throws Exception {
        ChannelConfig channelConfig = new ChannelConfig();
        List<String> mhdVersions = Arrays.asList(MhdIgImplEnum.MHDv410.getIgName());
        channelConfig.setXdsSiteName(FhirToolkitCapabilityStatement.XDS_COMPREHENSIVE_META_SIM);
        channelConfig.setCcFhirIgName(mhdVersions.toArray(new String[mhdVersions.size()]));
        channelConfig.setActorType("fhir");
        channelConfig.setChannelType("mhd");
        channelConfig.setChannelName("v410xds");
        CapabilityStatement capabilityStatement = getCapabilityStatement(ServicePropertiesEnum.MHD_CAPABILITY_STATEMENT_FILE, channelConfig);
        assert capabilityStatement != null;
        assert capabilityStatement.hasRest();
        String profile = new MhdV410CanonicalUriCodes().getUriCodeMap().get(CanonicalUriCodeEnum.COMPREHENSIVE);
        assert profile.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getDocumentation());
        assert CapabilityStatement.SystemRestfulInteraction.TRANSACTION.equals( capabilityStatement.getRest().get(0).getInteraction().get(0).getCode());
    }



    private CapabilityStatement getCapabilityStatement(ServicePropertiesEnum spEnum, ChannelConfig channelConfig) throws Exception {
        BaseResource baseResource = FhirToolkitCapabilityStatement.getCapabilityStatement(spEnum, channelConfig);
        assert baseResource != null;
        assert baseResource instanceof CapabilityStatement;
        return (CapabilityStatement)baseResource;

//        String content = ParserBase.encode(baseResource, Format.XML);
//        assert content.indexOf("${") == -1; // Parameters should have been replaced. If not check ServicePropertiesEnum to make sure the parameter have a matching key.
    }
}
