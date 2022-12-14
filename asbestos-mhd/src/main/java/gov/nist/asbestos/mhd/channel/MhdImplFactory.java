package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.mhd.transforms.MhdV3x;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.mhd.transforms.MhdV410;

import java.util.logging.Logger;

public class MhdImplFactory {
    private static Logger logger = Logger.getLogger(MhdImplFactory.class.getName());

    public static MhdIgInterface getImplementation(MhdIgImplEnum mhdIgEnum ) {
        switch (mhdIgEnum) {
            case MHDv3x:
                return new MhdV3x();
            case MHDv4:
                return new MhdV4();
            case MHDv410:
                return new MhdV410();
        }
        logger.warning("Unknown or Null mhdIgEnum.");
        return null;
    }
}
