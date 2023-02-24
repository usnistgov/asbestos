package gov.nist.asbestos.testEngine.engine;

import java.util.regex.Pattern;

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
        String[] delimiters =  {"/","\\"};
        for (String d : delimiters) {
            if (s.indexOf(d) > -1) {
                String arr[] = s.split(Pattern.quote(d));
                return arr[arr.length - 1].replace(".xml", "");
            }
        }
            return s;
    }
}
