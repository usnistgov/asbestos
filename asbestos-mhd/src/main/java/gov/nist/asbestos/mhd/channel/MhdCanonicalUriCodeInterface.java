package gov.nist.asbestos.mhd.channel;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ListResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public interface MhdCanonicalUriCodeInterface {
    static final Logger privateLogger = Logger.getLogger(MhdCanonicalUriCodeInterface.class.getName());
    static final List<MhdIgImplEnum> ANY_VERSION = null;
//    String mhdProfileCanonicalUriExceptionStr = "Method must be hidden by implementation through a static method.";
//    static Map<CanonicalUriCodeEnum, String> getAll() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
//    static Map<CanonicalUriCodeEnum, String> getProfiles() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
    /*
    default Map.Entry<CanonicalUriCodeEnum,String> getBundleProfile(CanonicalUriCodeEnum profileEnum) {
        Map<CanonicalUriCodeEnum,String> theMap = MhdProfileVersionInterface.getCanonicalUriMap(getMhdVersion().getMhdImplClass());
        for (Map.Entry<CanonicalUriCodeEnum, String> mbp : theMap.entrySet()) {
            if (mbp.getKey().equals(profileEnum)) {
                return mbp;
            }
        }
        return null;
    }

     */
    default Map.Entry<CanonicalUriCodeEnum, String> detectBundleProfileType(Bundle bundle) throws Exception {
        try {
            if (bundle.getMeta().getProfile().size() != 1) {
                throw new Exception("Bundle.meta.profile was not declared properly in bundle. Expected profile count size is 1.");
            }

            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            Map<CanonicalUriCodeEnum,String> theMap = getUriCodesByType(UriCodeTypeEnum.PROFILE); // MhdProfileVersionInterface.getCanonicalUriMap(getMhdVersion().getMhdImplClass());
            for (Map.Entry<CanonicalUriCodeEnum,String> me : theMap.entrySet()) {
                if (me.getValue().equals(bundleProfile.asStringValue())) {
                    return me;
                }
            }
        } catch (Exception e) {
            throw new Exception("Bundle.meta.profile missing? Exception: " + e.toString());
        }
        throw new Exception("Channel did not recognize Bundle.meta.profile, check channel configuration FHIR IG version support.");
    }

    Map<CanonicalUriCodeEnum, String> getUriCodesByType(UriCodeTypeEnum codeTypeEnum);
    Map<CanonicalUriCodeEnum, String> getUriCodeMap();


    static boolean isCodedAsAListType(List<MhdIgImplEnum> mhdVersionEnumList, BaseResource resource, CanonicalUriCodeEnum code) {
        if (resource instanceof ListResource) {
            ListResource listResource = (ListResource) resource;

            if (mhdVersionEnumList == ANY_VERSION) {
                mhdVersionEnumList = Arrays.asList(MhdIgImplEnum.values());
            }

            for (MhdIgImplEnum mhdVersionEnum : mhdVersionEnumList) {
                Class<? extends MhdCanonicalUriCodeInterface> myUriCodesClass = mhdVersionEnum.getUriCodesClass();
                try {
                    try {
                        String system = myUriCodesClass.getDeclaredConstructor().newInstance().getUriCodeMap().get(code);
                        if (system != null) {
                            if (listResource.getCode().hasCoding(system, code.getCode())) {
                                /*
                                http://hl7.org/fhir/R4/datatypes-definitions.html#Coding.system
                                Check if cardinality is [1..1]
                                */
                                if (listResource.getCode().getCoding().stream().filter(e -> system.equals(e.getSystem()) && code.toString().equals(e.getCode())).count() == 1) {
                                    return true;
                                }
                            }
                        }


                    } catch (Exception ex) {
                        privateLogger.warning("getAll() method call failed: " + ex.toString());
                    }
                } catch (Exception ex) {
                    privateLogger.warning(ex.toString());
                }
            }
        }
        return false;
    }

}
