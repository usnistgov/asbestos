package gov.nist.asbestos.client.client;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.simapi.tk.installation.Installation;

import java.io.File;

public class FhirClientBuilder {

    public static FhirClient get(String channelId) {
        FhirClient fhirClient = new FhirClient();
        File externalCache = Installation.instance().externalCache();
//        ResourceCacheMgr cmgr = new ResourceCacheMgr(externalCache);
//        fhirClient.setResourceCacheMgr(cmgr);
        EC ec = new EC(externalCache);

        File alternatePatientCacheDir = ec.getTestLogCacheDir("default__default");
        ResourceCacheMgr cmgr = new ResourceCacheMgr(alternatePatientCacheDir, new Ref(""));
        alternatePatientCacheDir.mkdirs();
        fhirClient.setResourceCacheMgr(cmgr);

        if (channelId != null) {
            File patientCacheDir = ec.getTestLogCacheDir(channelId);
            patientCacheDir.mkdirs();
            fhirClient.getResourceCacheMgr().addCache(patientCacheDir);
        }

        return fhirClient;
    }
}
