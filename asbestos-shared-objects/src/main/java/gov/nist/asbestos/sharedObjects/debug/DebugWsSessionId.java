package gov.nist.asbestos.sharedObjects.debug;

public class DebugWsSessionId {
    private String wsSessionId;
    private String testScriptIndex;

    public DebugWsSessionId(String wsSessionId, String testScriptIndex) {
        this.wsSessionId = wsSessionId;
        this.testScriptIndex = testScriptIndex;
    }

    public String getWsSessionId() {
        return wsSessionId;
    }

    public String getTestScriptIndex() {
        return testScriptIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DebugWsSessionId that = (DebugWsSessionId) o;

        return testScriptIndex != null ? testScriptIndex.equals(that.testScriptIndex) : that.testScriptIndex == null;
    }

    @Override
    public int hashCode() {
        return testScriptIndex != null ? testScriptIndex.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DebugWsSessionId{" +
                "wsSessionId='" + wsSessionId + '\'' +
                ", testScriptIndex='" + testScriptIndex + '\'' +
                '}';
    }

    public String getQuotedIdentifier() {
       return "\"" + getTestScriptIndex() + "\"";
    }
}
