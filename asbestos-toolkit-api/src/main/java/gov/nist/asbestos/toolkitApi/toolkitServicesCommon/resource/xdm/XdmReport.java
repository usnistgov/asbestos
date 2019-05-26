package gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.xdm;

import java.util.List;

/**
 *
 */
public interface XdmReport {
    boolean isPass();
    String getReport();
    List<XdmItem> getItems();
}
