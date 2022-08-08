package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.simapi.validation.Val;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MhdV410 implements MhdProfileVersionInterface {
    private static final String SUBMISSION_SET_PROFILE = "https://profiles.ihe.net/ITI/MHD/4.0.1/StructureDefinition-IHE.MHD.Minimal.SubmissionSet.html#profile";
    public static final Map<String, String> listTypeMap  =
            Collections.unmodifiableMap(Stream.of(
                    new AbstractMap.SimpleEntry<>("submissionset", "https://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes"))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));


    public MhdV410(Val val, MhdTransforms mhdTransforms) {
    }
}
