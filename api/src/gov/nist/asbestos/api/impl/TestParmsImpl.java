package gov.nist.asbestos.api.impl;

import gov.nist.asbestos.api.TestParms;

public class TestParmsImpl implements TestParms {
    private boolean useJson = true;
    private boolean useGzip = false;

    @Override
    public TestParms useJSON() {
        useJson = true;
        return this;
    }

    @Override
    public TestParms useXML() {
        useJson = false;
        return this;
    }

    @Override
    public TestParms useGzip() {
        useGzip = true;
        return this;
    }

    public boolean isUseJson() {
        return useJson;
    }

    public boolean isUseXML() {
        return !useJson;
    }

    public boolean isUseGzip() {
        return useGzip;
    }
}
