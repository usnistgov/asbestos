package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

/**
 *
 */
public interface RegistryError {
    String getErrorCode();

    String getErrorContext();

    String getLocation();

    ResponseStatusType getStatus();
}
