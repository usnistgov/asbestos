package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdCanonicalUriCodeInterface;
import gov.nist.asbestos.mhd.channel.UriCodeTypeEnum;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MhdV4CanonicalUriCodes implements MhdCanonicalUriCodeInterface {
    private static String comprehensiveMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle";
    private static String minimalMetadataProfile = "http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle";
    private final Map<CanonicalUriCodeEnum, String> canonicalUriCodeEnumStringMap =
            Collections.unmodifiableMap(Stream.of(
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.SUBMISSIONSET, "http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"),
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.COMPREHENSIVE, comprehensiveMetadataProfile),
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.MINIMAL, minimalMetadataProfile),
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.IHESOURCEIDEXTENSION, "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId"),
                    new AbstractMap.SimpleEntry<>(CanonicalUriCodeEnum.IHEDESIGNATIONTYPEEXTENSIONURL, "http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType"))
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
