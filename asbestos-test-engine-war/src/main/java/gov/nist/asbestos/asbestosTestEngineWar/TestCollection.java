package gov.nist.asbestos.asbestosTestEngineWar;

import java.util.List;

public interface TestCollection {
    String getName();
    String getDescription();
    String getType();
    List<String> getTestNames();
}
