package gov.nist.asbestos.testEngine.engine;


import java.util.Objects;

public enum FtkInternalRequestCode {
   FTK_LOAD_FIXTURE("ftkLoadFixture"),
   FTK_FUNCTION_CODE("ftkFunctionCode"),
   FTK_FUNCTION_CODE_FN_PARAM("fn"),
   FTK_FUNCTION_CODE_FN_GET_CHANNEL_BASE("getFtkChannelBase"),
   FTK_FUNCTION_CODE_FN_GET_CHANNEL_FHIR_BASE("getFtkChannelFhirBase"),
   FTK_FUNCTION_CODE_CHANNELID_PARAM("FhirValidationChannelId"),
   FTK_FUNCTION_CODE_FN_GET_VALIDATION_BASE("getFtkValidationBase"),
   FTK_GET_EVENT_PART("eventPart");

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
