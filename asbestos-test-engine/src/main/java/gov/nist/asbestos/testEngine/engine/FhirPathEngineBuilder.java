package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;

public class FhirPathEngineBuilder {

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
