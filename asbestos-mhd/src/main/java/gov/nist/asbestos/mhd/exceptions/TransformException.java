package gov.nist.asbestos.mhd.exceptions;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

public class TransformException extends RuntimeException {
    private BaseResource response;

    public TransformException(BaseResource response) {
        this.response = response;
    }

    public TransformException(OperationOutcome response) {
        this.response = response;
    }

    public BaseResource getResponse() {
        return response;
    }
}
