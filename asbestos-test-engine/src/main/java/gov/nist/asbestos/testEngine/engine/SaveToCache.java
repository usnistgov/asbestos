package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URI;

public class SaveToCache extends GenericSetupAction {

    SaveToCache(ActionReference actionReference, FixtureMgr fixtureMgr, boolean isFollowedByAssert) {
        super(actionReference,isFollowedByAssert);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {

        if (!preExecute(op, operationReport))
            return;

        String channelId = getTestEngine().getChannelId();
        if (!op.hasSourceId()) {
            reporter.reportError("SourceId attribute not found");
            return;
        }
        String sourceIdString = op.getSourceId();

        // url is the cache file name to be built without the .json extension
        if (!op.hasUrl()) {
            reporter.reportError("url attribute not found");
            return;
        }
        String url = op.getUrl();

        url = variableMgr.updateReference(url);

        /**
         * Need:
         * channelId
         * resource
         * EC
         */

        EC ec = getTestEngine().getEC();

        FixtureComponent sourceId = fixtureMgr.get(sourceIdString);
        ResourceWrapper wrapper = sourceId.getResourceWrapper();
        String type = sourceId.getResponseType();
        if (!"Bundle".equals(type)) {
            reporter.reportError("SaveToCache: source must be a Bundle");
            return;
        }
        Bundle bundle = (Bundle) wrapper.getResource();
        String resourceType;
        if (bundle.getTotal() == 1) {
            Resource resource = bundle.getEntry().get(0).getResource();
            resourceType = resource.getClass().getSimpleName();
        } else {
            resourceType = "Bundle";
        }

        File typeBase = ec.getCache(channelId, resourceType);
        File outFile = new File(typeBase, url + ".json");
        String json = ProxyBase.encode(wrapper.getResource(), Format.JSON);

        try (PrintStream ps = new PrintStream(outFile)) {
            ps.println(json);
        } catch (FileNotFoundException e) {
            reporter.reportError("Write to cache failed: cannot write to " + outFile);
        }

        postExecute(wrapper, operationReport, isFollowedByAssert);
    }

    @Override
    String resourceTypeToSend() {
        return null;
    }

    @Override
    Ref buildTargetUrl() {
        return new Ref("");  // never user, must pass non-null test
    }

    SaveToCache setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SaveToCache setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }
}
