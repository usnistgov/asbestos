package gov.nist.asbestos.mhd.transactionSupport;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;

import java.io.ByteArrayOutputStream;

/**
 *
 */
public class PnrWrapper {

    /**
     * Build SOAP Envelope
     * @param toAddr
     * @param registryObjectList
     * @param documentDefinitions
     * @return xml string
     */
    public static String wrap(String toAddr, RegistryObjectListType registryObjectList, String documentDefinitions) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            new RegistryObjectListTypeBuilder().toOutputStream(registryObjectList, os);
            String xml = new String(os.toByteArray());
            String part1 = (String) PnrWrapper.class.getResource("/pnr/part1.txt").getContent();
            part1 = part1.replace("TO_ADDR", toAddr);
            String part2 = (String) PnrWrapper.class.getResource("/pnr/part2.txt").getContent();
            String part3 = (String) PnrWrapper.class.getResource("/pnr/part3.txt").getContent();
            return part1 + xml + part2 + documentDefinitions + part3;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
