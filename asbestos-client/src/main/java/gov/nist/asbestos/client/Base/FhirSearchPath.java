package gov.nist.asbestos.client.Base;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;

import java.io.File;

// TODO - this is repeated in several other places, sometimes split among multiple classes - needs cleanup
public class FhirSearchPath {

    static public FhirClient getFhirClient(EC ec, File testDef, String channelId) {

        FhirClient fhirClient = new FhirClient();
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClient.setResourceCacheMgr(inTestResources);

        File patientCacheDir = channelId == null ? null : ec.getTestLogCacheDir(channelId);
        if (patientCacheDir != null) {
            patientCacheDir.mkdirs();
            fhirClient.getResourceCacheMgr().addCache(patientCacheDir);
        }

        File alternatePatientCacheDir = ec.getTestLogCacheDir("default__default");
        alternatePatientCacheDir.mkdirs();
        fhirClient.getResourceCacheMgr().addCache(alternatePatientCacheDir);

        return fhirClient;
    }
}
