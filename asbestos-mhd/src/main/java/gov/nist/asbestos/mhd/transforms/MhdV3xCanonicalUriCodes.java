package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdCanonicalUriCodeInterface;
import gov.nist.asbestos.mhd.channel.UriCodeTypeEnum;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MhdV3xCanonicalUriCodes implements MhdCanonicalUriCodeInterface {
    static String comprehensiveMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle";
    static String minimalMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle";
    private final Map<CanonicalUriCodeEnum, String> canonicalUriCodeEnumStringMap =
            Collections.unmodifiableMap(Stream.of(
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.COMPREHENSIVE, comprehensiveMetadataProfile),
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.MINIMAL, minimalMetadataProfile))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));


    @Override
    public Map<CanonicalUriCodeEnum, String> getUriCodesByType(UriCodeTypeEnum uriCodeTypeEnum) {
        return canonicalUriCodeEnumStringMap.entrySet().stream()
                .filter(e -> uriCodeTypeEnum.equals(e.getKey().getType())).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @Override
    public Map<CanonicalUriCodeEnum, String> getUriCodeMap() {
        return canonicalUriCodeEnumStringMap;
    }

}
