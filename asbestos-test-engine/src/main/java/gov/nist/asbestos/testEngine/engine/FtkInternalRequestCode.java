package gov.nist.asbestos.testEngine.engine;


import java.util.Objects;

public enum FtkInternalRequestCode {
   LOAD_FTK_FIXTURE("loadFtkFixture"),
   GET_FTK_CHANNEL_FHIR_BASE("getFtkChannelFhirBase");

   private String code;

   FtkInternalRequestCode(String code) {
      this.code = code;
   }

   public String getCode() {
      return code;
   }

   static public FtkInternalRequestCode find(String s) {
      Objects.requireNonNull(s);
      for (FtkInternalRequestCode c : values()) {
         if (s.equals(c.getCode())) return c;
      }
      return null;
   }
}
