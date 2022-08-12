package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.transforms.MhdV3x;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.mhd.transforms.MhdV410;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.Bundle;

import java.util.logging.Logger;

public class MhdImplFactory {
    private static Logger logger = Logger.getLogger(MhdImplFactory.class.getName());

    public static MhdProfileVersionInterface getImplementation(MhdVersionEnum mhdVersionEnum, Val val, MhdTransforms mhdTransforms) {
        switch (mhdVersionEnum) {
            case MHDv3x:
                return new MhdV3x(val, mhdTransforms);
            case MHDv4:
                return new MhdV4(val, mhdTransforms);
            case MHDv410:
                return new MhdV410(val, mhdTransforms);
        }
        logger.warning("Unknown or Null mhdVersionEnum.");
        return null;
    }
}
