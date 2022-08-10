package gov.nist.asbestos.mhd.channel;

public interface MhdCanonicalUriCodeInterface {
    static final Logger privateLogger = Logger.getLogger(MhdProfileVersionInterface.class.getName());
    static final List<MhdVersionEnum> ANY_VERSION = null;
    String mhdProfileCanonicalUriExceptionStr = "Method must be hidden by implementation through a static method.";
    static Map<CanonicalUriCodeEnum, String> getAll() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
    static Map<CanonicalUriCodeEnum, String> getProfiles() {throw new RuntimeException(mhdProfileCanonicalUriExceptionStr);}
    String getIheReference();
    default Map.Entry<CanonicalUriCodeEnum,String> getBundleProfile(CanonicalUriCodeEnum profileEnum) {
        Map<CanonicalUriCodeEnum,String> theMap = MhdProfileVersionInterface.getCanonicalUriMap(getMhdVersion().getMhdImplClass());
        for (Map.Entry<CanonicalUriCodeEnum, String> mbp : theMap.entrySet()) {
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
            Map<CanonicalUriCodeEnum,String> theMap = MhdProfileVersionInterface.getCanonicalUriMap(getMhdVersion().getMhdImplClass());
            for (Map.Entry<CanonicalUriCodeEnum,String> me : theMap.entrySet()) {
                if (me.getKey().getType().equals("profile") && me.getValue().equals(bundleProfile.asStringValue())) {
                    return me.getKey();
                }
            }
        } catch (Exception e) {
            throw new Exception("Bundle.meta.profile missing? Exception: " + e.toString());
        }
        throw new Exception("Unrecognized bundle profile.");
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
