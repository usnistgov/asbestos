package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.transforms.MhdV3x;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.simapi.validation.Val;

public class MhdImplFactory {

    public static MhdProfileVersionInterface getImplementation(MhdVersionEnum mhdVersionEnum, Val val, MhdTransforms mhdTransforms) {
        if (MhdVersionEnum.MHDv3x.equals(mhdVersionEnum)) {
            return new MhdV3x(val, mhdTransforms);
        } else if (MhdVersionEnum.MHDv4.equals(mhdVersionEnum)) {
            return new MhdV4(val, mhdTransforms);
        }
        return null;
    }
}
