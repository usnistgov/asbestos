package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public interface MhdProfileVersionInterface {
    static final Logger privateLogger = Logger.getLogger(MhdProfileVersionInterface.class.getName());
    String getIheReference();

    default MhdCanonicalUriCodeInterface getUriCodesClass() {
        try {
            Class<? extends MhdCanonicalUriCodeInterface> myCodesClass = getMhdVersion().getUriCodesClass();
            return myCodesClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            privateLogger.warning("getUriCodesClass Exception: " + ex.toString());
            return null;
        }
    }

//    CanonicalUriCodeEnum getDetectedBundleProfile();

        default String getDocBase(String ref) {
        return String.format("%s/%s", getMhdVersion().getMhdDocBase(), ref);
    }


    List<Class<?>> getAcceptableResourceTypes();
    RegistryPackageType buildSubmissionSet(MhdTransforms mhdTransforms, ResourceWrapper wrapper, Val val, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities, CanonicalUriCodeEnum canonicalUriCodeEnum);
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


}
