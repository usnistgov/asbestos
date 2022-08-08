package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.mhd.transforms.MhdV410;
import gov.nist.asbestos.simapi.validation.ValE;
import net.sf.saxon.ma.map.MapFunctionSet;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MhdProfileVersionInterface {
    static final List<MhdVersionEnum> ANY_VERSION = null;
    String mhdProfileCanonicalUriExceptionStr = "Method must be hidden by implementation.";
    static Map<CanonicalUriCodeEnum, String> getAll() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
    static Map<CanonicalUriCodeEnum, String> getProfiles() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
    String getIheReference();
    default Map.Entry<CanonicalUriCodeEnum,String> getBundleProfile(CanonicalUriCodeEnum profileEnum) {
       for (Map.Entry<CanonicalUriCodeEnum, String> mbp : getProfiles().entrySet()) {
           if (mbp.getKey().equals(profileEnum)) {
               return mbp;
           }
       }
       return null;
    }
    // FIXME:
    // plan to is to consolidate all canonical uris in a impl-specific map
    default CanonicalUriCodeEnum detectBundleProfileType(Bundle bundle) throws Exception {
        try {
            if (bundle.getMeta().getProfile().size() != 1) {
                throw new Exception("Profile not declared properly in bundle. Expected profile count size is 1.");
            }

            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            for (Map.Entry<CanonicalUriCodeEnum,String> mbp : getAll().entrySet()) {
                if (mbp.equals(bundleProfile.asStringValue())) {
                   return mbp.getKey();
                }
            }
        } catch (Exception e) {
            throw new Exception("Bundle.meta.profile missing");
        }
        throw new Exception("Unrecognized bundle profile.");
    }
    CanonicalUriCodeEnum getDetectedBundleProfile();
    List<Class<?>> getAcceptableResourceTypes();
    RegistryPackageType buildSubmissionSet(ResourceWrapper wrapper, ValE vale, IdBuilder idBuilder, ChannelConfig channelConfig, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities);
    MhdTransforms getMhdTransforms();
    String getExtrinsicId(ValE valE, ResourceMgr rMgr, List<Identifier> identifiers);
    MhdVersionEnum getMhdVersion();

    default Optional<String> hasSsQueryParam(List<String> paramList) throws Exception {
        if (paramList == null) {
            throw new Exception(String.format("Search param cannot be empty or null. See %s/ITI-66.html#23664121-query-search-parameters", getMhdVersion().getMhdDocBase()));
        }
        Optional<String> matchParam = paramList.stream().filter(s -> s.contains("code=submissionset") || s.contains("code%3dsubmissionset")).findAny();
        if (! matchParam.isPresent()) {
            throw new Exception(String.format("Search param is empty or null. See %s/ITI-66.html#23664121-query-search-parameters", getMhdVersion().getMhdDocBase()));
        }
        return matchParam;
    }

    static boolean isCodedListType(List<MhdVersionEnum> mhdVersionEnumList, BaseResource resource, String code) {
        if (resource instanceof ListResource) {
            ListResource listResource = (ListResource)resource;
            List<Map<String, String>> listTypeMap = new ArrayList<>();

            for (MhdVersionEnum e : mhdVersionEnumList) {
                Class<? extends MhdProfileVersionInterface> intf = e.getMhdImplClass();


            }

            if (mhdVersionEnum == null || (mhdVersionEnum.contains(MhdVersionEnum.MHDv4))) {
                listTypeMap.add(MhdV4.getAll());
            }
            if (mhdVersionEnum == null || (mhdVersionEnum.contains(MhdVersionEnum.MHDv410))) {
                listTypeMap.add(MhdV410.listTypeMap);
            }

            for (Map<String, String> m : listTypeMap) {
                String system = m.get(code);
                if (listResource.getCode().hasCoding(system, code)) {
                    /* Check if cardinality is [1..1] */
                    if (listResource.getCode().getCoding().stream().filter(e -> system.equals(e.getSystem()) && code.equals(e.getCode())).count() == 1)  {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
