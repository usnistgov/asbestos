package gov.nist.asbestos.testEngine.engine;

public class MultiUseScriptAllocator {
    private String sourceScriptId;
    private String newScriptId;

    public MultiUseScriptAllocator(String sourceScriptId, String newScriptId) {
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
        return sourceScriptId.split("/")[1];
    }

    public String getNewComponentIdPart() {
        return newScriptId.split("/")[1];
    }
}
