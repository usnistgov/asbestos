package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.*;

import java.net.URI;
import java.util.Objects;

public class SetupActionMhdPdbTransaction extends SetupActionTransaction {
    private IdBuilder idBuilder = new IdBuilder(true);
    private int counter = 1;

    SetupActionMhdPdbTransaction(FixtureMgr fixtureMgr) {
        super(fixtureMgr);
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    @Override
    public void updateResourceToSend(BaseResource baseResource) {
        if (! (baseResource instanceof Bundle))
            return;
        Bundle bundle = (Bundle) baseResource;
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            if (!component.hasResource())
                continue;
            Resource resource = component.getResource();
            if (resource instanceof DocumentManifest) {
                DocumentManifest dm = (DocumentManifest) resource;
                if (dm.hasMasterIdentifier()) {
                    Identifier ident = dm.getMasterIdentifier();
                    String value = idBuilder.allocate(null);
                    value = value + "." + counter++;
                    ident.setValue(value);
                }
            } else if (resource instanceof DocumentReference) {
                DocumentReference dr = (DocumentReference) resource;
                if (dr.hasMasterIdentifier()) {
                    Identifier ident = dr.getMasterIdentifier();
                    String value = idBuilder.allocate(null); // maybe override
                    value = value + "." + counter++;
                    ident.setValue(value);
                }
            }
        }
    }

}
