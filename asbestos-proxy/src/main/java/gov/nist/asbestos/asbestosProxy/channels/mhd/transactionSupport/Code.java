package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport


public class Code {
    private String code
    private String codingScheme
    private String display
    private String system
    private boolean deprecated

    public Code (MarkupBuilder xml) {
        code = xml.@code
                codingScheme = xml.@codingScheme
                display = xml.@display
                system = xml.@system
                deprecated = xml.@deprecated
    }

    public String getCode() {
        return code;
    }

    public String getCodingScheme() {
        return codingScheme;
    }

    public String getDisplay() {
        return display;
    }

    public String getSystem() {
        return system;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}
