package gov.nist.asbestos.utilities;

public class RegError {
    private String msg;
    private ErrorType severity;

    public RegError(String msg, ErrorType severity) {
        this.msg = msg;
        this.severity = severity;
    }

    public String getMsg() {
        return msg;
    }

    public ErrorType getSeverity() {
        return severity;
    }

    public String toString() {
        return severity + " " + msg;
    }
}
