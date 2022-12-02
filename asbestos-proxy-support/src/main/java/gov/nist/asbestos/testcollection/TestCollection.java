package gov.nist.asbestos.testcollection;

import java.util.List;

public interface TestCollection {
    String getName();
    String getDescription();
    String getType();
    List<String> getTestNames();
}
