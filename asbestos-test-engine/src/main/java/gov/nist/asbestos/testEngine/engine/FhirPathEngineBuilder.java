package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;

class FhirPathEngineBuilder {

    static FHIRPathEngine build() {
        return new FHIRPathEngine(new HapiWorkerContext(ProxyBase.getFhirContext(), new PrePopulatedValidationSupport()));
    }

    public static boolean evalForBoolean(BaseResource resource, String expression) {
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

    public static String evalForString(BaseResource resource, String expression) {
        List<Base> results = build().evaluate(resource, expression);
        if (results.isEmpty())
            return null;
        Base result = results.get(0);
        if (result instanceof StringType) {
            return ((StringType) result).getValueAsString();
        }
        if (result instanceof UriType) {
            return ((UriType) result).getValueAsString();
        }
        if (result instanceof BooleanType)
            return ((BooleanType) result).getValueAsString();
        return null;
    }
}
