package gov.nist.asbestos.mhd.transactionSupport;

/**
 *
 */
public class Submission {
// TODO insert real To address


    public static String metadataInSoapWrapper(String registryObjectList, String documentDefinitions) {
        try {
            String header = (String) Submission.class.getResource("/submissionSoapHeader.txt").getContent();
            String trailer1 = (String) Submission.class.getResource("/submissionSoapTrailer.txt").getContent();
            String trailer2 = (String) Submission.class.getResource("/submissionSoapTrailer2.txt").getContent();
            return header + registryObjectList + trailer1 + documentDefinitions + trailer2;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
