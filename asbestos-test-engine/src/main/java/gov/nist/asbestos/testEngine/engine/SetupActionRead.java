package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

class SetupActionRead extends GenericSetupAction {

    String internalBasePath = null;

    SetupActionRead(ActionReference actionReference, FixtureMgr fixtureMgr, boolean isFollowedByAssert) {
        super(actionReference, isFollowedByAssert);
        this.fixtureMgr = fixtureMgr;
    }

    ResourceWrapper run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent opReport) {

        if (!preExecute(op, opReport))
            return null;

        ResourceWrapper wrapper = fhirClient.readResource(targetUrl, requestHeader);
        postExecute(wrapper, opReport, isFollowedByAssert);
        return wrapper;
    }

    // key difference between read and search
    boolean isSearchOk() {
        return false;
    }

    @Override
    String resourceTypeToSend() {
        if (op.hasResource()) {
            return op.getResource();
        }
        if (op.hasTargetId())
            return new Ref(op.getTargetId()).getResourceType();
        return null;
    }

    @Override
    Ref buildTargetUrl() {
        if (base == null)
            base = sut;

        // http://build.fhir.org/testscript-definitions.html#TestScript.setup.action.operation.params
        if (op.hasUrl()) {
            String theUrl = variableMgr.updateReference(op.getUrl());
            if (theUrl == null)
                return null;
            Ref url = new Ref(theUrl);
            if (url.isRelative()) {
                if (getInternalBasePath() != null) {
                    return new Ref(getInternalBasePath().concat(theUrl));
                } else {
                    return url.rebase(new Ref(base));
                }
            }
            return url;
        }
        // for READ this can only be ID
        if (op.hasParams()) {
            if (!op.hasResource()) {
                reporter.reportError( "has params but no resource");
                return null ;
            }
            String searchParms = prepParams();
            if (searchParms == null)
                return null;  // coding issue
            if (searchParms.startsWith("?") && !isSearchOk()) {
                reporter.reportError( "resulting URL is search (contains ?) - use search operation type instead");
                return null ;
            }

            Ref ref = new Ref(base, op.getResource(), searchParms);
            if (!isSearchOk() && !isReadable(ref)) {
                reporter.reportError("resulting URL is not complete - " + ref.asString());
                return null ;
            }
            return ref;
        }
        if (op.hasTargetId()) {
            Ref ref = refFromTargetId(op.getTargetId(), opReport, label);
            if (!isReadable(ref)) {
                reporter.reportError( "resulting URL is not complete - " + ref.asString());
                return null ;
            }
            return ref;
        }

        if (fixtureMgr.getLastOp() != null) {
            Ref ref = refFromTargetId(fixtureMgr.getLastOp(), opReport, label);
            if (!isReadable(ref)) {
                reporter.reportError( "resulting URL is not complete - " + ref.asString());
                return null ;
            }
            return ref;
        }
        reporter.reportError("Unable to construct URL for operation");
        return null;
    }

    boolean isReadable(Ref ref) {
        if (!ref.asString().startsWith("http")) return false;
        if (!ref.hasResource()) return false;
        if (!ref.hasId()) return false;
        return true;
    }

    // http://hl7.org/fhir/testscript-definitions.html#TestScript.setup.action.operation.params
    //  "/[id]/_history/[vid] {?_format=[mime-type]}"
    String prepParams() {
        String params = "";
        assert op.hasParams();
        boolean encodeRequestUrl = true;  // default is true
        if (op.hasEncodeRequestUrl())
            encodeRequestUrl = op.getEncodeRequestUrl();
        String rawParms1 = op.getParams();
        String rawParms = variableMgr.updateReference(rawParms1);
        if (rawParms == null) {
            reporter.reportError("Cannot resolve variable references in " + rawParms1);
            return null;
        }
        if (rawParms.startsWith("/"))
            rawParms = rawParms.substring(1);  // should only be ID and _format (this is a READ)
        Map<String, String> paramMap = Ref.parseParameters(rawParms);
        if (hasParamOtherthenId(paramMap))
            encodeRequestUrl = false;
        try {
            //searchParms.setParms(rawParms, encodeRequestUrl);
            params = encodeRequestUrl ? URLEncoder.encode(rawParms, "UTF-8") : rawParms;
        } catch (UnsupportedEncodingException e) {
            reporter.reportError("Unable to encode URL parameters - " + e.getMessage());
            return null;
        }
        return params;
    }

     boolean hasParamOtherthenId(Map<String, String> parms) {
        for (String name : parms.keySet()) {
            if (!"_format".equals(name))
                return true;
        }
        return false;
     }

    Ref refFromTargetId(String targetId, TestReport.SetupActionOperationComponent opReport, String label) {
        Ref ref = null;
        FixtureComponent fixture  = fixtureMgr.get(targetId);
        if (fixture != null && fixture.hasHttpBase()) {
            String location = null;
            if (fixture.getHttpBase() instanceof HttpPost) {
                location = ((HttpPost) fixture.getHttpBase()).getLocationHeader().getValue();
            } else if (fixture.getHttpBase() instanceof HttpGetter) {
                location = fixture.getHttpBase().getUri().toString();
            }
            if (location == null) {
                reporter.reportError( "targetId does not have id and type");
                return null;
            }
            Ref targetRef = new Ref(location);
            if (base == null)
                ref = targetRef;
            else
                ref = targetRef.rebase(base);
        }
        return ref;
    }

    SetupActionRead setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupActionRead setBase(URI base) {
        this.base = base;
        return this;
    }

    public SetupActionRead setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }

    public SetupActionRead setVariableMgr(VariableMgr variableMgr) {
        super.setVariableMgr(variableMgr);
        return this;
    }

    public SetupActionRead setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public SetupActionRead setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public SetupActionRead setType(String type) {
        this.type = type;
        return this;
    }

    public String getInternalBasePath() {
        return internalBasePath;
    }

    public void setInternalBasePath(String internalBasePath) {
        this.internalBasePath = internalBasePath;
    }
}
