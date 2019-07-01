package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

class FhirPathEngineBuilder {

    static FHIRPathEngine build() {
        return new FHIRPathEngine(new HapiWorkerContext(ProxyBase.getFhirContext(), new PrePopulatedValidationSupport()));
    }
}
