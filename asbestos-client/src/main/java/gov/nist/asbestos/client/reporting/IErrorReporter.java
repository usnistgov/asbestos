package gov.nist.asbestos.client.reporting;

public interface IErrorReporter {
    void requireNonNull(Object o, String msg);
    void requireNull(Object o, String msg);
}
