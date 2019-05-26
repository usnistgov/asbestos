package gov.nist.asbestos.toolkitApi.toolkitApi;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.xdm.XdmReport;
import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.xdm.XdmRequest;

/**
 *
 */
public interface XdmValidator {
    XdmReport validate(XdmRequest request) throws ToolkitServiceException;
}
