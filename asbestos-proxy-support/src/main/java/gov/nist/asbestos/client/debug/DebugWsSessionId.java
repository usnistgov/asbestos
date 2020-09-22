package gov.nist.asbestos.client.debug;

import java.util.Objects;

public class DebugWsSessionId implements Comparable<DebugWsSessionId> {
    /**
     * Optional
     */
    private String wsSessionId;
    /**
     * Required
     */
    private String testScriptIndex;

    /**
     *
     * @param wsSessionId
     * @param testScriptIndex
     */
    public DebugWsSessionId(String wsSessionId, String testScriptIndex) {
        this(testScriptIndex);
        this.wsSessionId = wsSessionId;
    }

    /**
     *
     * @param testScriptIndex
     */
    public DebugWsSessionId(String testScriptIndex) {
        Objects.requireNonNull(testScriptIndex);
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



    @Override
    public int compareTo(DebugWsSessionId o) {
        return this.testScriptIndex.compareTo(o.testScriptIndex);
    }
}
