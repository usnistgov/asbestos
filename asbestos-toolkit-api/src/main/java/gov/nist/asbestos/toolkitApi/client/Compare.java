package gov.nist.asbestos.toolkitApi.client;

import java.util.List;

public interface Compare {
    String getComparisonObjectType();
    List<Difference> compare(MetadataObject a, MetadataObject b);
}
