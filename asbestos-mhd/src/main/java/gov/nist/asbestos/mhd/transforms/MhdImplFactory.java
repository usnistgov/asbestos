package gov.nist.asbestos.mhd.transforms;

import gov.nist.asbestos.simapi.validation.Val;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MhdImplFactory {

    public static MhdProfileVersionInterface getImplementation(MhdVersionEnum mhdVersionEnum, Val val, MhdTransforms mhdTransforms) {
        if (MhdVersionEnum.MHDv3x.equals(mhdVersionEnum)) {
            return new MhdV3x(val, mhdTransforms);
        } else if (MhdVersionEnum.MHDv4.equals(mhdVersionEnum)) {
            return new MhdV4(val, mhdTransforms);
        }
        throw new NotImplementedException();
    }
}
