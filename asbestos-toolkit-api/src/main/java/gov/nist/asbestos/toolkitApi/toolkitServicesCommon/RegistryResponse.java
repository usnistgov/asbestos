package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

import java.util.List;

/**
 *
 */
public interface RegistryResponse {
    ResponseStatusType getStatus();
    List<RegistryError> getErrorList();
}
