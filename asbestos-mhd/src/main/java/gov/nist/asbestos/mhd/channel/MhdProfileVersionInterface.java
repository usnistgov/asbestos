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
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public interface MhdProfileVersionInterface {
    static final Logger privateLogger = Logger.getLogger(MhdProfileVersionInterface.class.getName());
    static final List<MhdVersionEnum> ANY_VERSION = null;
    String mhdProfileCanonicalUriExceptionStr = "Method must be hidden by implementation through a static method.";
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

    default CanonicalUriCodeEnum detectBundleProfileType(Bundle bundle) throws Exception {
        try {
            if (bundle.getMeta().getProfile().size() != 1) {
                throw new Exception("Profile not declared properly in bundle. Expected profile count size is 1.");
            }

            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            for (Map.Entry<CanonicalUriCodeEnum,String> me : getProfiles().entrySet()) {
                if (me.equals(bundleProfile.asStringValue())) {
                   return me.getKey();
                }
            }
        } catch (Exception e) {
            throw new Exception("Bundle.meta.profile missing");
        }
        throw new Exception("Unrecognized bundle profile.");
    }

    /**
     *
     * @return null if unknown type
     */
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

    static Map<CanonicalUriCodeEnum, String> getCanonicalUriMap(Class<? extends MhdProfileVersionInterface> myClass) {
        try {
            Method m = myClass.getDeclaredMethod("getAll", null);
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

    static boolean isCodedListType(List<MhdVersionEnum> mhdVersionEnumList, BaseResource resource, String code) {
        if (resource instanceof ListResource) {
            ListResource listResource = (ListResource) resource;

            if (mhdVersionEnumList == ANY_VERSION) {
                mhdVersionEnumList = Arrays.asList(MhdVersionEnum.values());
            }

            for (MhdVersionEnum mhdVersionEnum : mhdVersionEnumList) {
                Class<? extends MhdProfileVersionInterface> myClass = mhdVersionEnum.getMhdImplClass();
                try {
                    if (myClass != null) {
                        try {
                            Map<CanonicalUriCodeEnum, String> map = getCanonicalUriMap(myClass);
                            for (Map.Entry<CanonicalUriCodeEnum, String> me : map.entrySet()) {
                                if (me.getKey().getType().equals(code)) {
                                    String system = me.getValue();
                                    if (listResource.getCode().hasCoding(system, code)) {
                                    /*
                                    http://hl7.org/fhir/R4/datatypes-definitions.html#Coding.system
                                    Check if cardinality is [1..1]
                                    */
                                        if (listResource.getCode().getCoding().stream().filter(e -> system.equals(e.getSystem()) && code.equals(e.getCode())).count() == 1) {
                                            return true;
                                        }
                                    }

                                }
                            }

                        } catch (Exception ex) {
                            privateLogger.warning("getAll() method call failed: " + ex.toString());
                        }
                    } else {
                    }
                } catch (Exception ex) {
                    privateLogger.warning(ex.toString());
                }
            }
        }
        return false;
    }

}
