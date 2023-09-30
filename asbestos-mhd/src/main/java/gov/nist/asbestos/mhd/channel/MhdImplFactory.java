package gov.nist.asbestos.mhd.channel;

/*
import gov.nist.asbestos.mhd.transforms.MhdV3x;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.mhd.transforms.MhdV410;
 */

import java.util.logging.Logger;

public class MhdImplFactory {
    private static Logger logger = Logger.getLogger(MhdImplFactory.class.getName());

    public static MhdIgInterface getImplementation(MhdIgImplEnum mhdIgEnum ) {
        if (mhdIgEnum == null) {
            logger.severe("Null mhdIgEnum.");
            return null;
        }
        Class<? extends MhdIgInterface> mhdIgInterface = mhdIgEnum.getMhdImplClass();
        if (mhdIgInterface == null) {
            logger.severe("Null mhdIgInterface.");
            return null;
        }
        try {
            return mhdIgInterface.newInstance();
        } catch (Exception ex) {
            logger.severe("Exception in newInstance: " + ex.toString());
            return null;
        }
        /*
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
         */
    }
}
