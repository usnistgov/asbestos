package gov.nist.asbestos.client.resolver;

import gov.nist.asbestos.client.client.FhirClient;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * load by a factory - either TestResourceCacheFactory or ResourceCacheMgrFactory
 *
 * Manages multiple resource caches.  Each cache is identified by its BaseUrl
 *
 */
public class ResourceCacheMgr {
    private static final Logger logger = Logger.getLogger(ResourceCacheMgr.class);

    private Map<Ref, CacheBundle> caches = new HashMap<>();  // baseUrl -> cache
    private List<Ref> knownPatientServers = new ArrayList<>();
    private FhirClient fhirClient = null;

    public ResourceCacheMgr(File externalCache) {
        Objects.requireNonNull(externalCache);
        assert externalCache.isDirectory();
        loadCache(new File(externalCache, "resourceCache"));
        loadPatientServers(externalCache);
    }

    private void loadPatientServers(File externalCache) {
        File patientServersFile = new File(externalCache, "patientServers.txt");
        try (Stream<String> stream = Files.lines(Paths.get(patientServersFile.toString()))) {
            stream.forEach(line -> {
                String theLine = line.trim();
                if (!line.equals("")) {
                    knownPatientServers.add(new Ref(line).getBase());
                }
            });
        } catch (IOException e) {
            // ignore
        }
    }

    // dir is File based backing cache
    public ResourceCacheMgr(File dir, Ref baseUrl) {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(baseUrl);
        assert dir.exists() && dir.isDirectory();
        CacheBundle cacheBundle = new CacheBundle(dir, baseUrl);
        caches.put(cacheBundle.getBase(), cacheBundle);
    }

    public void addCache(File dir) {
        //if (caches.get(new Ref("")) == null)

        caches.get(new Ref("")).file.addCache(dir);
    }

    public List<Ref> getCachedServers() {
        return new ArrayList<>(caches.keySet());
    }

    private void loadCache(File cacheCollection) {
        if (cacheCollection.exists() && cacheCollection.isDirectory()) {
            File[] dirs = cacheCollection.listFiles();
            if (dirs == null)
                return;
            for (File dir : dirs) {
                if (dir.isDirectory() && new File(dir, "cache.properties").exists()) {
                    CacheBundle cacheBundle = new CacheBundle(dir);
                    Ref base = cacheBundle.getBase();
                    caches.put(base, cacheBundle);
                }
            }
        }
    }

    // add to memory cache
    public void add(Ref uri, IBaseResource resource) {
        if (!uri.isAbsolute())
            throw new RuntimeException("ResourceCacheMgr#add: cannot add " + uri + " - must be absolute URI");
        Ref fhirbase = uri.getBase();
        CacheBundle cacheBundle = caches.get(fhirbase);
        if (cacheBundle == null) {
            MemoryResourceCache mcache = new MemoryResourceCache();
            mcache.add(uri, new ResourceWrapper(resource));
            caches.put(fhirbase, new CacheBundle(mcache));
        }
        cacheBundle.mem.add(uri, new ResourceWrapper(resource));
    }

    public ResourceWrapper getResource(Ref fullUrl) {
        Objects.requireNonNull(fullUrl);
        CacheBundle cache = caches.get(fullUrl.getBase());
        if (cache == null)
            return null;
        return cache.getResource(fullUrl);
    }


    List<ResourceWrapper> searchForPatient(String system, String value) {
        List<ResourceWrapper> results = new ArrayList<>();
        for (Ref ref : caches.keySet()) {
            CacheBundle cacheBundle = caches.get(ref);
            List<ResourceWrapper> patients = cacheBundle.mem.getAll("Patient");
            for (ResourceWrapper wrapper : patients) {
                Patient patient = (Patient) wrapper.getResource();
                if (hasIdentifier(patient, system, value))
                    results.add(wrapper);
            }
        }
        if (!results.isEmpty())
            return results;

        for (Ref patientServer : knownPatientServers) {
            List<ResourceWrapper> patientWrappers = fhirClient.search(patientServer, Patient.class, Collections.singletonList("Patient.identifier=" + system + "|" + value), true, false);
            if (!patientWrappers.isEmpty())
                return patientWrappers;
        }
        return results;  // empty
    }

    private boolean hasIdentifier(Patient patient, String system, String value) {
        for (Identifier identifier : patient.getIdentifier()) {
            if (identifier.hasValue() && identifier.hasSystem()) {
                if (system.equals(identifier.getSystem()) && value.equals(identifier.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<ResourceWrapper> search(Ref base, Class<?> resourceType, List<String> params, boolean stopAtFirst) {
        if (params.size() > 1)
            throw new Error("Don't support search params " + params);
        if (resourceType != Patient.class)
            throw new Error("Don't support search on resources other than Patient (search was for " + resourceType.getSimpleName() + ")" );
        String param = params.get(0);
        String[] parts = param.split("=");
        if (parts.length != 2)
            throw new Error("Don't understand params = " + param);
        if (!parts[0].equals("identifier"))
            throw new Error("Don't support param " + parts[0]);
        String systemAndId = parts[1];
        if (!systemAndId.contains("|"))
            throw new Error("Param format (" + systemAndId + ") not supported");
        String[] sparts = systemAndId.split("\\|");
        String system = sparts[0];
        String id = sparts[1];

        List<ResourceWrapper> results = new ArrayList<>();
        for (Ref ref : caches.keySet()) {
            CacheBundle cache = caches.get(ref);
            Ref aBase = ref.getBase();
            List<ResourceWrapper> all = cache.getAll(aBase, resourceType.getSimpleName());
            if (stopAtFirst && !all.isEmpty()) {
                results.add(all.get(0));
                return results;
            }
            results.addAll(all);
        }
        return results;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName() + "\n");
        caches.forEach((Ref key, CacheBundle value) -> {
            buf.append(key).append(" => ").append(value).append("\n");
        });

        return buf.toString();
    }

    public void addKnownPatientServer(Ref server) {
        server = server.getBase();
        if (!knownPatientServers.contains(server))
            knownPatientServers.add(server);
    }

    public ResourceCacheMgr setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }
}
