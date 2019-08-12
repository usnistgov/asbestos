package gov.nist.asbestos.client.resolver;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatientCacheMgr {

    public class PatientCacheItem {
        String systemAndValue;   // system|value
        ResourceWrapper wrapper;
        LocalDateTime expires;

        PatientCacheItem(Patient patient, Ref ref, String systemAndValue) {
            Objects.requireNonNull(ref);
            Objects.requireNonNull(patient);
            this.wrapper = new ResourceWrapper(patient, ref);
            this.systemAndValue = systemAndValue;
            this.expires = LocalDateTime.now().plusSeconds(lifetime);
        }

        PatientCacheItem(ResourceWrapper wrapper, String systemAndValue) {
            this((Patient) wrapper.getResource(), wrapper.getRef(), systemAndValue);
        }

        public Ref getRef() {
            return wrapper.getRef();
        }

        public Patient getPatient() {
            return (Patient) wrapper.getResource();
        }

        public ResourceWrapper getWrapper() {
            return new ResourceWrapper(getPatient(), getRef());
        }
    }

    private List<PatientCacheItem> cache = new ArrayList<>();
    private int lifetime = 30;  // seconds

    public PatientCacheItem find(String systemAndValue) {
        LocalDateTime now = LocalDateTime.now();
        List<PatientCacheItem> deleteable = new ArrayList<>();
        PatientCacheItem found = null;
        for (PatientCacheItem ele : cache) {
            if (ele.systemAndValue.equals(systemAndValue)) {
                if (ele.expires.isBefore(now))
                    deleteable.add(ele);
                else {
                    if (found == null)
                        found = ele;
                }
            }
        }
        cache.removeAll(deleteable);
        return found;
    }

    public PatientCacheItem find(Ref ref) {
        LocalDateTime now = LocalDateTime.now();
        List<PatientCacheItem> deleteable = new ArrayList<>();
        PatientCacheItem found = null;
        for (PatientCacheItem ele : cache) {
            if (ele.getRef().equals(ref)) {
                if (ele.expires.isBefore(now))
                    deleteable.add(ele);
                else {
                    if (found == null)
                        found = ele;
                }
            }
        }
        cache.removeAll(deleteable);
        return found;
    }

    private void delete(String systemAndValue) {
        PatientCacheItem ele = find(systemAndValue);
        if (ele != null)
            cache.remove(ele);
    }

    public PatientCacheMgr add(ResourceWrapper wrapper, String systemAndValue) {
        delete(systemAndValue);
        PatientCacheItem ele = new PatientCacheItem(wrapper, systemAndValue);
        cache.add(ele);
        return this;
    }

    public PatientCacheMgr add(ResourceWrapper wrapper) {
        if (!(wrapper.getResource() instanceof Patient))
            return this;
        Patient patient = (Patient) wrapper.getResource();
        for (Identifier identifier : patient.getIdentifier()) {
            if (identifier.hasSystem() && identifier.hasValue()) {
                String system = identifier.getSystem();
                if (system.startsWith("urn:oid:"))
                    system = system.substring("urn:oid:".length());
                add(wrapper, system + "|" + identifier.getValue());
            }
        }

        return this;
    }

    PatientCacheMgr setLifeTime(int seconds) {
        this.lifetime = seconds;
        return this;
    }

}
