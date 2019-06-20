package gov.nist.asbestos.mhd.transactionSupport;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;

/**
 *
 */
public class PnrWrapper {

    public static String wrap(String toAddr, RegistryObjectListType registryObjectList, String documentDefinitions) {
        try {
            String part1 = (String) PnrWrapper.class.getResource("/pnr/part1.txt").getContent();
            part1 = part1.replace("TO_ADDR", toAddr);
            String part2 = (String) PnrWrapper.class.getResource("/pnr/part2.txt").getContent();
            String part3 = (String) PnrWrapper.class.getResource("/pnr/part3.txt").getContent();
            return part1 + registryObjectList + part2 + documentDefinitions + part3;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
