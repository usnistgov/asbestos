package gov.nist.asbestos.asbestosProxy.wrapper;

import gov.nist.asbestos.client.client.Format;
import org.hl7.fhir.r4.model.BaseResource;

public class TransformException extends RuntimeException {
    private BaseResource response;

    public TransformException(BaseResource response) {
        this.response = response;
    }

    public BaseResource getResponse() {
        return response;
    }
}
