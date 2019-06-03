package gov.nist.asbestos.simapi.tk.actors;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransactionType {
    private String id = "";
    private String name = "";
    private String shortName = "";
    private String code = "";   // like pr.b - used in actors table
    private String asyncCode = "";
    private boolean needsRepUid = false;  // I think maybe not used? RM
    private String requestAction = "";
    private String responseAction = "";
    private boolean requiresMtom = false;
    private boolean http = false; // Is this Http only (non-SOAP) actor
    private boolean fhir = false;
    private String endpointSimPropertyName;  // TODO is this irrelevant?
    private String tlsEndpointSimPropertyName;  // TODO is this irrelevant?
    private FhirVerb fhirVerb = FhirVerb.NONE;

    static List<TransactionType> types = new ArrayList<>();

    private static void init(File ec) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        File typesDir = new File(new File(ec, "types"), "transactions");
        File[] files = typesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith("json")) continue;
                TransactionType type = objectMapper.readValue(file, TransactionType.class);
                types.add(type);
            }
        }
    }

   @Override
    public String toString() { return shortName; }

    public boolean isFhir() { return fhir;  }

    public boolean isRequiresMtom() {
        return requiresMtom;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCode() {
        return code;
    }

    public String getAsyncCode() {
        return asyncCode;
    }

    public boolean usesTraditionalTransactions() {
        if (requestAction.equals("")) return false;
        return true;
    }

    public FhirVerb getFhirVerb() {
        return fhirVerb;
    }

    /**
    * @return the {@link #requestAction} value.
    */
    public String getRequestAction() {
      return requestAction;
   }

   /**
    * @return the {@link #responseAction} value.
    */
   public String getResponseAction() {
      return responseAction;
   }

    public boolean isHttpOnly() {
      return http;
   }

    public boolean equals(TransactionType tt) {
        return name.equals(tt.name);
	}

    // if lookup by id is needed, must also select off of receiving actor
    public static  TransactionType find(String s) {
        if (s == null) return null;
        for (TransactionType t : types) {
            if (s.equals(t.name)) return t;
            if (s.equals(t.shortName)) return t;
            if (s.equals(t.code)) return t;
            if (s.equals(t.asyncCode)) return t;
            if (s.equals(t.getId())) return t;
        }
        return null;
    }

    public static  TransactionType find(String s, FhirVerb fhirVerb) {
        if (s == null) return null;
        for (TransactionType t : types) {
            if (s.equals(t.name) && t.fhirVerb == fhirVerb) return t;
            if (s.equals(t.shortName) && t.fhirVerb == fhirVerb) return t;
            if (s.equals(t.code) && t.fhirVerb == fhirVerb) return t;
            if (s.equals(t.asyncCode) && t.fhirVerb == fhirVerb) return t;
            if (s.equals(t.getId()) && t.fhirVerb == fhirVerb) return t;
        }
        return null;
    }

    public boolean isIdentifiedBy(String s) {
        if (s == null) return false;
        return
				s.equals(id) ||
						s.equals(name) ||
						s.equals(shortName) ||
						s.equals(code) ||
						s.equals(asyncCode);
    }

    public static  TransactionType findByRequestAction(String action) {
		if (action == null) return null;
		for (TransactionType t : types) {
			if (action.equals(t.requestAction)) return t;
		}
		return null;
	}

    public static  TransactionType findByResponseAction(String action) {
		if (action == null) return null;
		for (TransactionType t : types) {
			if (action.equals(t.responseAction)) return t;
		}
		return null;
	}

    public static  List<TransactionType> asList() {
        List<TransactionType> l = new ArrayList<TransactionType>();
        for (TransactionType t : types)
            l.add(t);
        return l;
    }

    public String getEndpointSimPropertyName() {
        return endpointSimPropertyName;
    }

    public String getTlsEndpointSimPropertyName() {
        return tlsEndpointSimPropertyName;
    }
}
