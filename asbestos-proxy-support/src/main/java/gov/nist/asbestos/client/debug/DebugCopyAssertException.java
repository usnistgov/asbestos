package gov.nist.asbestos.client.debug;


public class DebugCopyAssertException extends Exception {
    private String propKey;

   public DebugCopyAssertException(String message, String propKey) {
        super(message);
        this.propKey = propKey;
   }

    public String getPropKey() {
        return propKey;
    }
}
