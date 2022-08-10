package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public interface MhdProfileVersionInterface {
    static final Logger privateLogger = Logger.getLogger(MhdProfileVersionInterface.class.getName());
    static final String GET_ALL_URI_CODES = "getAllUriCodes";
    static final String GET_URI_CODES = "getUriCodes";
    String getIheReference();


    default String getDocBase(String ref) {
        return String.format("%s/%s", getMhdVersion().getMhdDocBase(), ref);
    }


    List<Class<?>> getAcceptableResourceTypes();
    RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities);
    MhdTransforms getMhdTransforms();
    String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers);
    MhdVersionEnum getMhdVersion();

    default Optional<String> hasSsQueryParam(List<String> paramList) throws Exception {
        if (paramList == null) {
            throw new Exception(String.format("Search param cannot be empty or null. See %s/ITI-66.html#23664121-query-search-parameters", getMhdVersion().getMhdDocBase()));
        }
        final String ssCode = CanonicalUriCodeEnum.SUBMISSIONSET.getCode();
        Optional<String> matchParam = paramList.stream().filter(s -> s.contains("code=" + ssCode) || s.contains("code%3d" + ssCode)).findAny();
        if (! matchParam.isPresent()) {
            throw new Exception(String.format("Search param is empty or null. See %s/ITI-66.html#23664121-query-search-parameters", getMhdVersion().getMhdDocBase()));
        }
        return matchParam;
    }

    /*
    static Map<CanonicalUriCodeEnum, String> getCanonicalUriMap(Class<? extends MhdProfileVersionInterface> myClass) {
        try {
            Method m = myClass.getDeclaredMethod(GET_ALL_URI_CODES, null);
            if (m != null) {
                try {
                    return (Map<CanonicalUriCodeEnum, String>) m.invoke(null, null);
                } catch (Exception ex) {
                    privateLogger.warning("getCanonicalUriMap Invoke Exception" + ex.toString());
                }
            }
        } catch (Exception ex) {
            privateLogger.warning("getCanonicalUriMap getDeclaredMethod Exception" + ex.toString());
        }
        return null;
    }

    static Map<CanonicalUriCodeEnum, String> getCanonicalUriMapByType(Class<? extends MhdProfileVersionInterface> myClass, UriCodeTypeEnum uriCodeTypeEnum) {
        try {
            Method m = myClass.getDeclaredMethod(GET_URI_CODES, UriCodeTypeEnum.class);
            if (m != null) {
                try {
                    return (Map<CanonicalUriCodeEnum, String>) m.invoke(null, uriCodeTypeEnum);
                } catch (Exception ex) {
                    privateLogger.warning("getCanonicalUriMapByType Invoke Exception" + ex.toString());
                }
            }
        } catch (Exception ex) {
            privateLogger.warning("getCanonicalUriMapByType getDeclaredMethod Exception" + ex.toString());
        }
        return null;
    }
     */




}
