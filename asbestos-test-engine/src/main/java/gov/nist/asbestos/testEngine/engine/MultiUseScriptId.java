package gov.nist.asbestos.testEngine.engine;

public class MultiUseScriptId {
    private String sourceScriptId;
    private String newScriptId;

    public MultiUseScriptId(String sourceScriptId, String newScriptId) {
        this.sourceScriptId = sourceScriptId;
        this.newScriptId = newScriptId;
    }


    public String getSourceScriptId() {
        return sourceScriptId;
    }

    public String getNewScriptId() {
        return newScriptId;
    }

    public String getSourceComponentIdPart() {
        return getComponentPart(sourceScriptId);
    }

    public String getNewComponentIdPart() {
        return getComponentPart(newScriptId);
    }

    public static String getComponentPart(String s) {
        if (s.indexOf("/") > -1)
            return s.split("/")[1];
        else
            return s;
    }
}
