package gov.nist.asbestos.utilities;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class PnrWrapper {

    public static String wrap(String toAddr, String soapBody) {
        try {

            String header = (String) IOUtils.toString((InputStream)PnrWrapper.class.getResource("/pnr/SoapHeader.txt").getContent(), StandardCharsets.UTF_8);
            header = header.replace("TO_ADDR", toAddr);
            String footer = (String) IOUtils.toString((InputStream)PnrWrapper.class.getResource("/pnr/SoapFooter.txt").getContent(), StandardCharsets.UTF_8);
            return header + soapBody + footer;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
