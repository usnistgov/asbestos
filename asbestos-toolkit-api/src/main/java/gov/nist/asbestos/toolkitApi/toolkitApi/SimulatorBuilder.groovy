package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.SimConfig
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.SimId
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.resource.SimConfigResource
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.resource.SimIdResource
import groovy.transform.TypeChecked

/**
 * Build/modify a collection of different Actor simulators running in a remote copy of toolkit.
 */
@TypeChecked
 class SimulatorBuilder {
    String urlRoot;
    EngineSpi engine;

    /**
    * @return EngineSpi for this builder.
    */
    EngineSpi getEngine() {
       return engine;
   }


    /**
     * This will initialize the API to contact the test engine at
     * http://hostname:port/xdstools2
     * @param urlRoot where engine is running - typical value would be http://localhost:8080/xdstools2
     */
     SimulatorBuilder(String urlRoot) {
        this.urlRoot = urlRoot;
        engine = new EngineSpi(urlRoot);
    }

    // These are private because you should use the actor-specific call like createRespondingGateway
    private SimConfig create(BasicSimParameters p) throws ToolkitServiceException {
        return create(p.getId(), p.getUser(), p.getActorType(), p.getEnvironmentName());
    }

    private SimConfig create(String id, String user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType actorType, String environmentName) throws ToolkitServiceException {
        return engine.create(id, user, actorType, environmentName);
    }

    /**
     * Create new Document Source simulator with default configuration.
     * To create a simulator with
     * custom configuration:
     * <ol>
     * <li>Create simulator with default configuration</li>
     * <li>Update the local copy of the configuration</li>
     * <li>Send the update via the update method</li>
     * </ol>
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param environmentName Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
     DocumentSource createDocumentSource(String id, String user, String environmentName) throws ToolkitServiceException {
        XdrDocumentSource src = new XdrDocumentSource();
        src.engine = engine;
        src.config =  engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.DOCUMENT_SOURCE, environmentName);
        SimConfig sc = src.config;
        ((SimConfigResource)sc).setEnvironmentName(environmentName);
        return src;
    }

    /**
     * Create new Imaging Document Source simulator with default configuration.
     * To create a simulator with
     * custom configuration:
     * <ol>
     * <li>Create simulator with default configuration</li>
     * <li>Update the local copy of the configuration</li>
     * <li>Send the update via the update method</li>
     * </ol>
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param environmentName Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
     ImagingDocumentSource createImagingDocumentSource(String id, String user, String environmentName) throws ToolkitServiceException {
        XdsiImagingDocumentSource src = new XdsiImagingDocumentSource();
        src.engine = engine;
        src.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.DOCUMENT_SOURCE, environmentName);
        return src;
    }

    /**
     * Create new Document Consumer simulator with default configuration.
     * To create a simulator with
     * custom configuration:
     * <ol>
     * <li>Create simulator with default configuration</li>
     * <li>Update the local copy of the configuration</li>
     * <li>Send the update via the update method</li>
     * </ol>
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param environmentName Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
     DocumentConsumer createDocumentConsumer(String id, String user, String environmentName) throws ToolkitServiceException {
        XdsDocumentConsumer cons = new XdsDocumentConsumer();
        cons.engine = engine;
        cons.config =  engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.DOCUMENT_CONSUMER, environmentName);
        return cons;
    }
    /**
     * Create new Imaging Document Consumer simulator with default configuration.
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param env Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
//     ImagingDocumentConsumer createImagingDocumentConsumer(String id,
//       String user, String env) throws ToolkitServiceException {
//       XdsiImagingDocumentConsumer idc = new XdsiImagingDocumentConsumer();
//       idc.engine = engine;
//       idc.config = engine.create(id, user, SimulatorActorType.IMAGE_DOCUMENT_CONSUMER, env);
//       return idc;
//    }

    /**
     * Create new Document Recipient simulator with default configuration.
     * To create a simulator with
     * custom configuration:
     * <ol>
     * <li>Create simulator with default configuration</li>
     * <li>Update the local copy of the configuration</li>
     * <li>Send the update via the update method</li>
     * </ol>
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param environmentName Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
     DocumentRecipient createDocumentRecipient(String id, String user, String environmentName) throws ToolkitServiceException {
        XdrDocumentRecipient act = new XdrDocumentRecipient();
        act.engine = engine;
        act.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.DOCUMENT_RECIPIENT, environmentName);
        return act;
    }

     IFhirServer createFhirServer(String id, String user, String environmentName) throws ToolkitServiceException {
        FhirServer server = new FhirServer();
        server.engine = engine;
        server.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.FHIR_SERVER, environmentName);
        return server;
    }


    /**
     * Create new Document Registry/Repository simulator with default configuration.
     * To create a simulator with
     * custom configuration:
     * <ol>
     * <li>Create simulator with default configuration</li>
     * <li>Update the local copy of the configuration</li>
     * <li>Send the update via the update method</li>
     * </ol>
     * @param id Simulator ID
     * @param user User creating Simulator.  Same as TestSession in Toolkit UI. The simulator ID must be unique for this user.
     * @param environmentName Environment defines Affinity Domain coding schemes and TLS certificate for use with client.
     * @return Simulator configuration.
     * @throws ToolkitServiceException if anything goes wrong.
     */
     DocumentRegRep createDocumentRegRep(String id, String user, String environmentName) throws ToolkitServiceException {
        XdsDocumentRegRep act = new XdsDocumentRegRep();
        act.engine = engine;
        act.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.REPOSITORY_REGISTRY, environmentName);
        return act;
    }

     DocumentRepository createDocumentRepository(String id, String user, String environmentName)  throws ToolkitServiceException {
        XdsDocumentRepository act = new XdsDocumentRepository();
        act.engine = engine;
        act.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.REPOSITORY, environmentName);
        return act;
    }

     RespondingGateway createRespondingGateway(String id, String user, String environmentName) throws ToolkitServiceException {
        XcaRespondingGateway act = new XcaRespondingGateway();
        act.engine = engine;
        act.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.RESPONDING_GATEWAY, environmentName);
        return act;
    }

     InitiatingGateway createInitiatingGateway(String id, String user, String environmentName) throws ToolkitServiceException {
        XcaInitiatingGateway act = new XcaInitiatingGateway();
        act.engine = engine;
        act.config = engine.create(id, user, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorActorType.INITIATING_GATEWAY, environmentName);
        return act;
    }

     InitiatingGateway asInitiatingGateway(SimConfig config) {
        XcaInitiatingGateway act = new XcaInitiatingGateway();
        act.engine = engine;
        act.config = config;
        return act;
    }

    /**
     * Update the configuration of a Simulator. Any properties that are passed in ChannelConfig that are
     * not recognized will be silently ignored. Parameters passed with wrong type (String vs. boolean) will cause
     * ToolkitServiceException.
     *
     * Expected usage is to retrieve the configuration using the get() method,
     * update the parameters, and then submit the update using this call.
     * @param config new configuration
     * @return updated ChannelConfig if updates made or null if no changes accepted.
     * @throws ToolkitServiceException if anything goes wrong
     */
     SimConfig update(SimConfig config) throws ToolkitServiceException {
        return engine.update(config);
    }

    /**
     * Delete a simulator. There is another call available using the SimId parameter type.  This
     * parameter type contains the raw ID and USER that are used here.  The two calls function identically.
     * If the simulator does not exist no error is returned.
     * @param id of simulator
     * @param user of simulator
     * @throws ToolkitServiceException if anything goes wrong.
     */
     void delete(String id, String user) throws ToolkitServiceException {
        engine.delete(id, user);
    }

    void delete(BasicSimParameters p) throws ToolkitServiceException {
        delete(p.getId(), p.getUser());
    }


    /**
     * Delete a simulator. There is another call available using separate raw ID and USER parameters.
     * USER and ID are components of the more formal SimId type.  The two calls functionally identically.
     * If the simulator does not exist no error is returned.
     * @param simId Simulator ID
     * @throws ToolkitServiceException if anything goes wrong
     */
     void delete(SimId simId) throws ToolkitServiceException {
        engine.delete(simId);
    }

    /**
     * Get configuration of a Simulator.
     * @param simId simulator ID
     * @return simulator configuration
     * @throws ToolkitServiceException if anything goes wrong
     */
     SimConfig get(SimId simId) throws ToolkitServiceException {
        return engine.get(simId);
    }

   /**
    * Creates SimId instance for passed user (session) and proxy id. Note: Does
    * not check to see if such a simulator exists.
    *
    * @param user (session)
    * @param id simulator id.
    * @return SimId instance.
    */
    SimId get(String user, String id) {
        SimIdResource simId = new SimIdResource();
        simId.setUser(user);
        simId.setId(id);
        return simId;
    }

     XdmValidator createXdmValidator() {
        XdmValidatorImpl impl = new XdmValidatorImpl();
        impl.engine = engine;
        return impl;
    }

}
