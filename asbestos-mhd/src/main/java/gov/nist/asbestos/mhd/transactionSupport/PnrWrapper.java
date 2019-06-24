package gov.nist.asbestos.mhd.transactionSupport;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class PnrWrapper {

    public static String wrap(String toAddr, String soapBody) {
        try {

            String header = (String) IOUtils.toString((InputStream)PnrWrapper.class.getResource("/pnr/header.txt").getContent(), StandardCharsets.UTF_8);
            header = header.replace("TO_ADDR", toAddr);
            String footer = (String) IOUtils.toString((InputStream)PnrWrapper.class.getResource("/pnr/footer.txt").getContent(), StandardCharsets.UTF_8);
            return header + soapBody + footer;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
