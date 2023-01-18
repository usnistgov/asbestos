package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class FhirPathEngineBuilder {
    private static Logger logger = Logger.getLogger(FhirPathEngineBuilder.class.getName());

    static class FtkStringFunctions {
        /**
         * base64 string decode function is not available in FHIRPath 2.0.0, hence this utility class is needed.
         *
         * https://build.fhir.org/ig/HL7/FHIRPath/#additional-string-functions
         * FHIRPath Continuous Build (v2.1.0)
         */
        static class FtkDecode {
            private static final String FUNCTION_ID = ".ftkDecode('base64')";
            static boolean isFtkDecodeFunction(String s) {
               return s.endsWith(FUNCTION_ID);
            }
            static String ftkDecode(String s) {
                return new String(Base64.getDecoder().decode(s));
            }

            public static String expressionBeforeDecode(String expression) {
                return expression.substring(0, expression.indexOf(FUNCTION_ID));
            }
        }
    }

    static FHIRPathEngine build() {
        return new FHIRPathEngine(new HapiWorkerContext(ParserBase.getFhirContext(), new PrePopulatedValidationSupport(ParserBase.getFhirContext())));
    }

    public static boolean evalForBoolean(BaseResource resource, String expression) {
        if (resource == null)
            return false;
        List<Base> results;
        try {
            results = build().evaluate(resource, expression);
        } catch (Throwable t) {
            throw t;
        }
        if (results.isEmpty())
            return false;
        Base result = results.get(0);
        if (result instanceof BooleanType) {
            boolean val = ((BooleanType) result).booleanValue();
            return val;
        }
        return true;
    }

    public static List<Base> evalForResources(Base resourceIn, String expression) {
        if (resourceIn == null)
            return null;
        List<Base> results = build().evaluate(resourceIn, expression);
        return results;
    }

    public static Resource evalForResource(Resource resourceIn, String expression) {
        if (resourceIn == null)
            return null;
        List<Base> results = build().evaluate(resourceIn, expression);
        if (results.isEmpty())
            return null;
        if (results.size() > 1)
            return null;
        Base result = results.get(0);
        if (result instanceof Bundle.BundleEntryComponent) {
            Bundle.BundleEntryComponent comp = (Bundle.BundleEntryComponent) result;
            return comp.getResource();
        }
        return null;
    }

    public static String evalForString(Base base) {
        return getStringValue(base);
    }

    public static String evalForString(BaseResource resource, String expression) {
        if (resource == null)
            return "";

        if (FtkStringFunctions.FtkDecode.isFtkDecodeFunction(expression)) {
            String expressionBeforeDecode = FtkStringFunctions.FtkDecode.expressionBeforeDecode(expression);
            List<Base> results = build().evaluate(resource, expressionBeforeDecode);
            if (results.isEmpty())
                return null;
            if (results.size() != 1) {
               logger.warning("ftkDecodeFunction unexpected result size: " + results.size());
               return null;
            }
            Base result = results.get(0);
            String s = getStringValue(result);
            return FtkStringFunctions.FtkDecode.ftkDecode(s);
        }

        List<Base> results = build().evaluate(resource, expression);
        if (results.isEmpty())
            return null;
        if (results.size() > 1) {
            StringBuilder buf = new StringBuilder();
            buf.append("[");
            boolean first = true;
            for (Base base : results) {
                if (first)
                    first = false;
                else
                    buf.append(", ");
                String simpleName = base.getClass().getSimpleName();
                if (simpleName.equals("StringType")) {
                    buf.append(simpleName).append("[").append(base.toString()).append("]");
                } else
                    buf.append(simpleName);
            }
            buf.append("]");
            return buf.toString();
        }
        Base result = results.get(0);
        return getStringValue(result);
    }

    private static String getStringValue(Base result) {
        if (result instanceof StringType) {
            return ((StringType) result).getValueAsString();
        }
        if (result instanceof UriType) {
            return ((UriType) result).getValueAsString();
        }
        if (result instanceof BooleanType) {
            return ((BooleanType) result).getValueAsString();
        }
        if (result instanceof UnsignedIntType) {
            return ((UnsignedIntType) result).getValueAsString();
        }
        if (result instanceof IntegerType) {
            return ((IntegerType) result).getValueAsString();
        }
        if (result instanceof DateType) {
            return ((DateType) result).getValueAsString();
        }
        String className = result.getClass().getSimpleName();
        if (className.endsWith("Info")) {
            StringBuilder buf = new StringBuilder();
            buf.append("[");
            buf.append("Class").append(": ").append(className).append("\n");
            Base base = (Base) result;
            buf.append("Name").append(": ").append(base.getUserString("name")).append("\n");
            for (Property property : base.children()) {
                buf.append(property.getName()).append(": ").append(property.toString()).append("\n");
            }
            buf.append("]");
            return buf.toString();
        }

        return className;
    }
}
