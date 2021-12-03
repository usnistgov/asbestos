package gov.nist.asbestos.testEngine.engine.translator;

import java.util.List;

public class Parameter {
    private String callerName;
    private String localName;
    private boolean isVariable = false;

    public String getCallerName() {
        return
                //isVariable ? "${" + callerName + "}" :
                callerName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    static public Parameter findByCallerName(List<Parameter> parameterList, String callerName) {
        for (Parameter p : parameterList) {
            if (p.getCallerName().equals(callerName))
                return p;
        }
        return null;
    }

    public static String extractParameterName(String localName) {
        if (isVariable(localName)) {
            localName = localName.substring(2);
            localName = localName.substring(0, localName.length() - 1);
            return localName;
        }
        return localName;
    }

    public static boolean isVariable(String name) {
       return name.startsWith("${") && name.endsWith("}");
    }

    static public Parameter findByLocalName(List<Parameter> parameterList, String localName) {
        boolean isVariable = isVariable(localName);
        if (isVariable) {
            localName = extractParameterName(localName);
        }
        for (Parameter p : parameterList) {
            if (p.getLocalName().equals(localName)) {
                p.isVariable = isVariable;
                return p;
            }
        }
        return null;
    }

    public boolean isVariable() {
        return isVariable;
    }

    public Parameter setVariable(boolean variable) {
        isVariable = variable;
        return this;
    }
}
