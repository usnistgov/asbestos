package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RefList
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.SimConfig
import groovy.transform.TypeChecked

/**
 *
 */
@TypeChecked
 interface AbstractActorInterface extends SimConfig {

    SimConfig getConfig();

     SimConfig update(SimConfig config) throws ToolkitServiceException;

    /**
     * Delete the actor.
     * @throws ToolkitServiceException if something goes wrong.
     */
     void delete() throws ToolkitServiceException;

    /**
     * Set a property that takes a String value
     * @param name property name. See {@link gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties} for property names.
     * @param value property value
     */
    void setProperty(String name, String value);
    /**
     * Set a property that takes a boolean value
     * @param name property name. See {@link gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties} for property names.
     * @param value property value
     */
    void setProperty(String name, boolean value);
    /**
     * Is named property a boolean value?
     * @param name property name. See {@link gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties} for property names.
     * @return boolean
     */
    boolean isBoolean(String name);
    /**
     * Return named property as a String
     * @param name property name. See {@link gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties} for property names.
     * @return String value
     */
    String asString(String name);
    /**
     * Return named property as a String
     * @param name property name. See {@link gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties} for property names.
     * @return boolean value
     */
     boolean asBoolean(String name);
    /**
     * Describe Simulator Configuration.
     * @return Description string.
     */
     String describe();

     String getId();

    RefList getEventIds(String simId, gov.nist.asbestos.simapi.tk.actors.TransactionType transaction) throws ToolkitServiceException;

    RefList getEvent(String simId, gov.nist.asbestos.simapi.tk.actors.TransactionType transaction, String eventId) throws ToolkitServiceException;

}
