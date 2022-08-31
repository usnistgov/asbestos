package gov.nist.asbestos.mhd.translation.attribute;

import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class PatientId implements IVal {
    private String patientid = "";
    private String aa = "";
    private String id = "";
    private ResourceCacheMgr resourceCacheMgr = null;
    private Val val;
    private FhirClient fhirClient = null;
    private Logger logger = Logger.getLogger(PatientId.class.getName());

    public PatientId setPatientid(String patientid) {
        this.patientid = patientid;
        this.aa = "";
        this.id = "";
        String[] parts = patientid.split("\\^\\^\\^");
        if (parts.length == 2) {
            id = parts[0].trim();
            String theAa = parts[1].trim();
            String[] aaParts = theAa.split("&");
            if (aaParts.length >= 2) {
                aa = aaParts[1].trim();
            }
        }
        return this;
    }

    public String getAa() {
        return aa;
    }

    public String getId() {
        return id;
    }

//    Ref patientServer = new Ref("http://localhost:8080/fhir/fhir");


    public Optional<Reference> getFhirReference() {
        Objects.requireNonNull(resourceCacheMgr);
        Objects.requireNonNull(val);
        Objects.requireNonNull(fhirClient);
        String system = "urn:oid:" + getAa();
        String id = getId();
        List<String> searchParams = new ArrayList<>();
        searchParams.add("identifier=" + system + "|" + id);
        Optional<String>[] patientServerBase = new Optional[]{
                ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.CAT_EXTERNAL_PATIENT_SERVER_FHIR_BASE),
                ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.HAPI_FHIR_BASE)
        };

        List<ResourceWrapper> results = null;
        for (Optional<String> o : patientServerBase) {
            try {
                if (o.isPresent()) {
                    Ref r = new Ref(o.get());
                    results = fhirClient.search(r, Patient.class, searchParams, true, false);
                    if (!results.isEmpty())
                        break;
                }
            } catch (Exception ex) {
                logger.warning(ex.toString());
            }
        }

        //List<ResourceWrapper> results = resourceCacheMgr.search(null, Patient.class, searchParams, true);
        if (results == null || (results !=null && results.isEmpty())) {
            val.add(new ValE("DocumentEntryToDocumentReference: cannot find Patient resource for " + system + "|" + id).asError());
            return Optional.empty();
        }
        return Optional.of(new Reference(results.get(0).getRef().toString()));
    }

    public PatientId setResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
        return this;
    }

    public void setVal(Val val) {
        this.val = val;
    }

    public PatientId setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }
}
