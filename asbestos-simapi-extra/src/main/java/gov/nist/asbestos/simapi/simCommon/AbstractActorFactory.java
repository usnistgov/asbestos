package gov.nist.asbestos.simapi.simCommon;


import gov.nist.asbestos.simapi.tk.actors.ActorType;
import gov.nist.asbestos.simapi.tk.actors.TransactionType;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.tk.installation.PropertyServiceManager;
import gov.nist.asbestos.simapi.tk.siteManagement.Site;
import gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;


/**
 * Factory class for simulators.  Technically ActorFactry is no longer accurate
 * since simulators are being built for things that are not IHE actors.
 * Oh well!
 * @author bill
 *
 */

 abstract class AbstractActorFactory {
	static Logger logger = Logger.getLogger(AbstractActorFactory.class);

	/*
	 *
	 *  Abstracts
	 *
	 */
	protected abstract Simulator buildNew(SimManager simm, SimId simId, String environment, boolean configureBase);
	protected abstract void verifyActorConfigurationOptions(SimulatorConfig config);
	 abstract Site buildActorSite(SimulatorConfig asc, Site site);
	 abstract List<TransactionType> getIncomingTransactions();

	private static boolean initialized = false;
	/**
	 * ActorType.name ==> ActorFactory
	 */
	static private Map<String, AbstractActorFactory> theFactories = null;
	private static Map<String, AbstractActorFactory> factories()  {
	 	if (theFactories != null) return theFactories;
		theFactories = new HashMap<>();

		 logger.info("Loading Actor Factories");

		// for this loader to work, the following requirements must be met by the factory class:
		// 1. Extend class AbstractActorFactory

		try {
			for (ActorType actorType : ActorType.types) {
				String factoryClassName = actorType.getSimulatorFactoryName();
				if (factoryClassName == null) continue;
				Class c = Class.forName(factoryClassName);
				AbstractActorFactory inf = (AbstractActorFactory) c.newInstance();
				logger.info("Loading ActorType " + actorType.getName());
				theFactories.put(actorType.getName(), inf);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		initialized = true;
		return theFactories;
	}

	Site getActorSite(SimulatorConfig asc, Site site)  {
		Site finalSite = buildActorSite(asc, site);
		if (finalSite == null) return null;
		finalSite.setOwner(asc.getTestSession().getValue());
		return finalSite;
	}

	 static boolean isInitialized() {
		return initialized;
	}

	 AbstractActorFactory getActorFactory(ActorType at) throws Exception {
		return factories().get(at.getName());
	}



    static final String name = "Name";
	static final String isTls = "UseTLS";
	static final String owner = "Owner";
	static final String description = "Description";

	private boolean transactionOnly = false;
	 boolean isSimProxy = false;

	 AbstractActorFactory asSimProxy() {
		isSimProxy = true;
		return this;
	}

	 AbstractActorFactory asNotSimProxy() {
		isSimProxy = false;
		return this;
	}

	PropertyServiceManager propertyServiceMgr = null;

	static  gov.nist.asbestos.simapi.tk.actors.ActorType getActorTypeFromName(String name) {
		return gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(name);
	}

	protected SimulatorConfig configureBaseElements(gov.nist.asbestos.simapi.tk.actors.ActorType simType, TestSession testSession, String environment) {
		return configureBaseElements(simType, null, testSession, environment);
	}

	protected SimulatorConfig configureBaseElements(gov.nist.asbestos.simapi.tk.actors.ActorType simType, SimId newId, TestSession testSession, String environment) {
		if (newId == null)
			newId = getNewId(testSession);
		SimulatorConfig sc = new SimulatorConfig(newId, simType.getShortName(), SimDb.getNewExpiration(SimulatorConfig.class), environment);

		return configureBaseElements(sc);
	}

	protected void configEnv(SimManager simm, SimId simId, SimulatorConfig sc) {
		if (simId.getEnvironmentName() != null) {
			gov.nist.asbestos.toolkitApi.envSettings.EnvSetting es = new gov.nist.asbestos.toolkitApi.envSettings.EnvSetting(simId.getEnvironmentName());
			File codesFile = es.getCodesFile();
			addEditableConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.codesEnvironment, ParamType.SELECTION, codesFile.toString());
		} else {
			File codesFile = gov.nist.asbestos.toolkitApi.envSettings.EnvSetting.getEnvSetting(simm.sessionId()).getCodesFile();
			addEditableConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.codesEnvironment, ParamType.SELECTION, codesFile.toString());
		}
	}

	SimulatorConfig configureBaseElements(SimulatorConfig sc) {
		SimulatorConfigElement ele;

		ele = new SimulatorConfigElement();
		ele.setName(SimulatorProperties.creationTime);
		ele.setType(ParamType.TIME);
		ele.setStringValue(new Date().toString());
		addFixed(sc, ele);

		ele = new SimulatorConfigElement();
		ele.setName(name);
		ele.setType(ParamType.TEXT);
		ele.setStringValue(sc.getId().toString());
		addFixed(sc, ele);

		addEditableConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.locked, ParamType.BOOLEAN, false);
		addEditableConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.requiresStsSaml, ParamType.BOOLEAN, false);
        addEditableConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.FORCE_FAULT, ParamType.BOOLEAN, false);
		addFixedConfig(sc, gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.environment, ParamType.TEXT, sc.getEnvironmentName());

        return sc;
	}

	protected AbstractActorFactory() {

	}

	// Returns list since multiple simulators could be built as a grouping/cluster
	// only used by SimulatorFactory to offer a generic API for building sims
	 Simulator buildNewSimulator(SimManager simm, String simtype, SimId simID, String environment, boolean save) throws Exception {
        logger.info("Build New Simulator " + simtype);
		ActorType at = ActorType.findActor(simtype);

		if (at == null)
			throw new RuntimeException("Simulator type [" + simtype + "] does not exist");

		return buildNewSimulator(simm, at, simID, environment, save);

	}

	gov.nist.asbestos.simapi.tk.actors.ActorType actorType = null;

	 gov.nist.asbestos.simapi.tk.actors.ActorType getActorType() {
		return actorType;
	}

	 Simulator buildNewSimulator(SimManager simm, gov.nist.asbestos.simapi.tk.actors.ActorType at, SimId simID, String environment, boolean save) throws Exception {
		logger.info("Build new Simulator of type " + getClass().getSimpleName() + " simID: " + simID);

		// This is the simulator-specific factory
        String actorTypeName;
		actorTypeName = at.getName();
		AbstractActorFactory af = factories().get(actorTypeName);
		actorType = gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(actorTypeName);

		if (af == null)
			throw new Exception(String.format("Cannot build simulator of type %s - cannot find Factory for ActorType", actorTypeName));

		af.actorType = actorType;

        if (simID.getId().contains("__"))
            throw new Exception("Simulator ID cannot contain double underscore (__)");

		Simulator simulator = af.buildNew(simm, simID, environment,true);

		if (simulator.size() > 1) {
			List<String> simIdsInGroup = new ArrayList<>();
			for (SimulatorConfig conf : simulator.getConfigs())
				simIdsInGroup.add(conf.getId().toString());
			for (SimulatorConfig conf : simulator.getConfigs()) {
				SimulatorConfigElement ele = new SimulatorConfigElement(gov.nist.asbestos.toolkitApi.configDatatypes.server.SimulatorProperties.simulatorGroup, ParamType.LIST, simIdsInGroup);
				conf.add(ele);
			}
		}

		// This is out here instead of being attached to a simulator-specific factory - why?
		if (save) {
			for (SimulatorConfig conf : simulator.getConfigs()) {
				AbstractActorFactory actorFactory = getActorFactory(conf);
				saveConfiguration(conf);

				BaseActorSimulator sim = RuntimeManager.getSimulatorRuntime(conf.getId());
				logger.info("calling onCreate:" + conf.getId().toString());
				sim.onCreate(conf);
			}

			if (isSimProxy) {
				for (SimulatorConfig conf : simulator.getConfigs()) {
					conf.getId().forFhir();  // label it FHIR so it gets re-saved there
					AbstractActorFactory actorFactory = getActorFactory(conf);
					saveConfiguration(conf);

					BaseActorSimulator sim = RuntimeManager.getSimulatorRuntime(conf.getId());
					logger.info("calling onCreate:" + conf.getId().toString());
					sim.onCreate(conf);
				}
			}
		}

		return simulator;
	}


	//
	// End of hooks


	// A couple of utility classes that get around a client class calling a server class - awkward
	static  gov.nist.asbestos.simapi.tk.actors.ActorType getActorType(SimulatorConfig config) {
		return gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(config.getActorType());
	}

	static  AbstractActorFactory getActorFactory(SimulatorConfig config) {
		gov.nist.asbestos.simapi.tk.actors.ActorType actorType = getActorType(config);
		String actorTypeName = actorType.getName();
		AbstractActorFactory actorFactory = factories().get(actorTypeName);
		return actorFactory;
	}

	 List<SimulatorConfig> checkExpiration(List<SimulatorConfig> configs) {
		List<SimulatorConfig> remove = new ArrayList<SimulatorConfig>();

		for (SimulatorConfig sc : configs) {
			if (sc.checkExpiration())
				remove.add(sc);
		}
		configs.removeAll(remove);
		return configs;
	}


	private SimId getNewId(TestSession testSession) {
		String id = gov.nist.asbestos.simapi.tk.util.UuidAllocator.allocate();
		String[] parts = id.split(":");
		id = parts[2];

            return new SimId(testSession, id);
	}

	String mkEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, boolean isTLS) throws Exception {
		return mkEndpoint(asc, ele, asc.getActorType().toLowerCase(), isTLS);
	}

	protected String mkEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, String actor, boolean isTLS) {
		return mkEndpoint(asc, ele, actor, isTLS, false);
	}

	protected String mkEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, String actor, boolean isTLS, boolean isProxy) {
		String transtype = SimDb.getTransactionDirName(ele.getTransactionType());

		String contextName = Installation.instance().getServletContextName();

		String tlsTail = isTLS ? "s" : "";

		String endpoint =  "http" + tlsTail +
				"://" +
				gov.nist.asbestos.simapi.tk.installation.Installation.instance().propertyServiceManager().getToolkitHost() +
				":" +
				getEndpointPort(isTLS, isProxy) +
				contextName +
				(ele.getTransactionType().isHttpOnly() ? "/httpsim/" : "/proxy/" ) +
				asc.getId() +
				"/" +
				actor +
				"/" +
				transtype;
		return endpoint;
	}

	protected String mkFhirEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, String actor, boolean isTLS) throws Exception {
		return mkFhirEndpoint(asc, ele, actor, null, isTLS, false);
	}

	protected String mkFhirEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, String actor, gov.nist.asbestos.simapi.tk.actors.TransactionType transactionType, boolean isTLS, boolean isProxy) throws Exception {

		String contextName = gov.nist.asbestos.simapi.tk.installation.Installation.instance().getServletContextName();

		String endpoint =  "http" +
				((isTLS) ? "s" : "") +
				"://" +
				gov.nist.asbestos.simapi.tk.installation.Installation.instance().propertyServiceManager().getToolkitHost() +
				":" +
				getEndpointPort(isTLS, isProxy) +
				contextName +
				((isSimProxy) ? "/proxy/" : "/fsim/") +
				asc.getId() +
				"/" + actor +
				((transactionType != null && transactionType.getFhirVerb() == gov.nist.asbestos.toolkitApi.configDatatypes.client.FhirVerb.TRANSACTION ? "/" + transactionType.getShortName() : ""))
				;
		return endpoint;
	}

	private String getEndpointPort(boolean isTLS, boolean isProxy) {
		if (isTLS && isProxy)
			throw new RuntimeException("Proxy does not support TLS");
		if (isProxy)
			return gov.nist.asbestos.simapi.tk.installation.Installation.instance().propertyServiceManager().getProxyPort();
		return (isTLS) ? gov.nist.asbestos.simapi.tk.installation.Installation.instance().propertyServiceManager().getToolkitTlsPort() : gov.nist.asbestos.simapi.tk.installation.Installation.instance().propertyServiceManager().getToolkitPort();
	}

	 void saveConfiguration(SimulatorConfig config) throws Exception {
		verifyActorConfigurationOptions(config);

		SimDb simdb = new SimDb().mkSim(config.getId(), config.getActorType());
		File simCntlFile = simdb.getSimulatorControlFile();
		SimulatorConfigIoFactory.impl().save(config, simCntlFile.toString());
	}

	static  void delete(SimulatorConfig config) throws Exception {
        delete(config.getId());
    }

    static  void delete(SimId simId) {
        logger.info("delete simulator " + simId);
		SimDb simdb;
			BaseActorSimulator sim = RuntimeManager.getSimulatorRuntime(simId);
			SimulatorConfig config = loadSimulator(simId, true);
			if (config != null)
				sim.onDelete(config);

			simdb = new SimDb(simId);
			simdb.delete();
	}

	static  List<TransactionInstance> getTransInstances(SimId simid, String xactor, String trans)
	{
		SimDb simdb;
		simdb = new SimDb(simid);
		gov.nist.asbestos.simapi.tk.actors.ActorType actor = simdb.getSimulatorActorType();
		return simdb.getTransInstances(actor.toString(), trans);
	}

	// update internal to proxy to align with current channelId
	static  void updateSimConfiguration(SimId simId) throws Exception {
		SimulatorConfig config = loadSimulator(simId, false);

		config.setId(simId);

		SimulatorConfigElement ele = config.getConfigEle(name);
		ele.setStringValue(simId.toString());

		new GenericSimulatorFactory().saveConfiguration(config);

		new SimDb(simId).updateSimConfiguration();
	}

	static  void renameSimFile(String simFileSpec, String newSimFileSpec)
			throws Exception {
		throw new Exception("Not Implemented");
	}

	static  List<SimulatorConfig> getSimConfigs(gov.nist.asbestos.simapi.tk.actors.ActorType actorType, TestSession testSession) {
		return getSimConfigs(actorType.getName(), testSession);
	}

	static  List<SimulatorConfig> getSimConfigs(String actorTypeName, TestSession testSession) {
		List<SimId> allSimIds = SimDb.getAllSimIds(testSession);
		List<SimulatorConfig> simConfigs = new ArrayList<>();

			for (SimulatorConfig simConfig : loadSimulators(allSimIds)) {
				if (actorTypeName.equals(simConfig.getActorType()))
					simConfigs.add(simConfig);
			}

		return simConfigs;
	}


	/**
	 * Load simulators - IOException if proxy not found
	 * @param ids
	 * @return
	 */
	static  List<SimulatorConfig> loadSimulators(List<SimId> ids)  {
		List<SimulatorConfig> configs = new ArrayList<SimulatorConfig>();

		for (SimId id : ids) {
			SimDb simdb = new SimDb(id);
			File simCntlFile = simdb.getSimulatorControlFile();
			SimulatorConfig config = restoreSimulator(simCntlFile.toString());
			configs.add(config);
		}

		return configs;
	}

	 List<SimulatorConfig> loadAvailableSimulators(List<SimId> ids) {
		List<SimulatorConfig> configs = new ArrayList<SimulatorConfig>();

		for (SimId id : ids) {
				if (!SimDb.exists(id)) continue;
				SimDb db = new SimDb(id);
				File simCntlFile = db.getSimulatorControlFile();
				SimulatorConfig config = restoreSimulator(simCntlFile.toString());
				configs.add(config);
		}

		return configs;
	}

	private static SimulatorConfig restoreSimulator(String filename) {
		return SimulatorConfigIoFactory.impl().restoreSimulator(filename);
	}

	 static SimulatorConfig loadSimulator(SimId simid, boolean okifNotExist) {
		SimDb simdb;
		File simCntlFile;
		try {
			if (SimDb.exists(simid)) {
				simdb = new SimDb(simid);
				simCntlFile = simdb.getSimulatorControlFile();
				SimulatorConfig config = restoreSimulator(simCntlFile.toString());
				return config;
			} else {
				return null;
			}
		} catch (Exception e) {
			if (okifNotExist) return null;
			throw new RuntimeException(e);
		}

	}

	static  SimulatorConfig getSimConfig(SimId simulatorId)  {
		assert SimDb.exists(simulatorId) : "No simulator for channelId: " + simulatorId.toString();
			SimDb simdb = new SimDb(simulatorId);
			File simCntlFile = simdb.getSimulatorControlFile();
			return restoreSimulator(simCntlFile.toString());
	}

	protected boolean isEndpointSecure(String endpoint) {
		return endpoint.startsWith("https");
	}

	protected List<SimulatorConfig> asList(SimulatorConfig asc) {
		List<SimulatorConfig> ascs = new ArrayList<SimulatorConfig>();
		ascs.add(asc);
		return ascs;
	}

	 void addFixed(SimulatorConfig sc, SimulatorConfigElement ele) {
		ele.setEditable(false);
		sc.elements().add(ele);
	}

	private void addUser(SimulatorConfig sc, SimulatorConfigElement ele) {
		ele.setEditable(true);
		sc.elements().add(ele);
	}

	 void addEditableConfig(SimulatorConfig sc, String name, ParamType type, Boolean value) {
		addUser(sc, new SimulatorConfigElement(name, type, value));
	}

	 void addEditableConfig(SimulatorConfig sc, String name, ParamType type, String value) {
		addUser(sc, new SimulatorConfigElement(name, type, value));
	}

	 void addEditableConfig(SimulatorConfig sc, String name, ParamType type, List<String> value) {
		addUser(sc, new SimulatorConfigElement(name, type, value));
	}

	 void addEditableConfig(SimulatorConfig sc, String name, ParamType type, List<String> values, boolean isMultiSelect) {
        addUser(sc, new SimulatorConfigElement(name, type, values, isMultiSelect));
    }

     void addEditableConfig(SimulatorConfig sc, String name, ParamType type, gov.nist.asbestos.toolkitApi.configDatatypes.client.PatientErrorMap value) {
        addUser(sc, new SimulatorConfigElement(name, type, value));
    }

	 void addFixedConfig(SimulatorConfig sc, String name, ParamType type, Boolean value) {
		addFixed(sc, new SimulatorConfigElement(name, type, value));
	}

	 void addFixedConfig(SimulatorConfig sc, String name, ParamType type, List<String> value) {
		addFixed(sc, new SimulatorConfigElement(name, type, value));
	}

	 void addFixedConfig(SimulatorConfig sc, String name, ParamType type, String value) {
		addFixed(sc, new SimulatorConfigElement(name, type, value));
	}

	 void setConfig(SimulatorConfig sc, String name, String value) {
		SimulatorConfigElement ele = sc.getUserByName(name);
		if (ele == null)
			throw new RuntimeException("Simulator " + sc.getId() + " does not have a parameter named " + name + " or cannot set its value");
		ele.setStringValue(value);
	}

	 void setConfig(SimulatorConfig sc, String name, Boolean value) {
		SimulatorConfigElement ele = sc.getUserByName(name);
		if (ele == null)
			throw new RuntimeException("Simulator " + sc.getId() + " does not have a parameter named " + name + " or cannot set its value");
		ele.setBooleanValue(value);
	}

	SimulatorConfigElement addEditableEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) {
		SimulatorConfigElement ele = new SimulatorConfigElement();
		ele.setName(endpointName);
		ele.setType(ParamType.ENDPOINT);
		ele.setTransactionType(transactionType);
		ele.setTls(tls);
		ele.setStringValue(mkEndpoint(sc, ele, actorType.getShortName(), tls));
		addUser(sc, ele);
		return ele;
	}

	 void addEditableNullEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) {
		SimulatorConfigElement ele = addEditableEndpoint(sc, endpointName, actorType, transactionType, tls);
		ele.setStringValue("");
	}

	 void addFixedEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) throws Exception {
		SimulatorConfigElement ele = new SimulatorConfigElement();
		 ele.setName(endpointName);
		 ele.setType(ParamType.ENDPOINT);
		 ele.setTransactionType(transactionType);
		ele.setStringValue(mkEndpoint(sc, ele, actorType.getShortName(), tls));
		ele.setTls(tls);
		addFixed(sc, ele);
	}

	 void addFixedFhirEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) throws Exception {
		addFixedFhirEndpoint(sc, endpointName, actorType, transactionType, tls, false);
	}

	 void addFixedFhirEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls, boolean proxy) throws Exception {
		SimulatorConfigElement ele = new SimulatorConfigElement();
		 ele.setName(endpointName);
		 ele.setType(ParamType.ENDPOINT);
		 ele.setTransactionType(transactionType);
		ele.setStringValue(mkFhirEndpoint(sc, ele, actorType.getShortName(), transactionType, tls, proxy));
		ele.setTls(tls);
		addFixed(sc, ele);
	}

	 boolean isTransactionOnly() {
		return transactionOnly;
	}

	 void setTransactionOnly(boolean transactionOnly) {
		this.transactionOnly = transactionOnly;
	}



}
