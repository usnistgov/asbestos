package gov.nist.asbestos.asbestosProxy.servlet;

import java.util.List;

public interface TestCollection {
    String getName();
    String getDescription();
    String getType();
    List<String> getTestNames();
}
