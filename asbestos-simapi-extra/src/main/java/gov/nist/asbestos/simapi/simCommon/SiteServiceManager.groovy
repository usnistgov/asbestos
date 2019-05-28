package gov.nist.asbestos.simapi.simCommon


import gov.nist.asbestos.simapi.tk.installation.Installation
import gov.nist.asbestos.simapi.tk.siteManagement.Site
import gov.nist.asbestos.simapi.tk.siteManagement.Sites
import gov.nist.asbestos.simapi.tk.stubs.SeparateSiteLoader;
import groovy.transform.TypeChecked;
import org.apache.log4j.Logger;

import javax.xml.parsers.FactoryConfigurationError;


/**
 * Top level site management API referenced by calls from the GUI.  One
 * instance is shared between all sessions. This guarantees that
 * common sites are common. Calls to get session specific simulators
 * are managed through this class. Those calls are passed through to
 * a session specific cache managed by SimManager.
 * @author bill
 *
 */
@TypeChecked
 class SiteServiceManager {
	private static SiteServiceManager siteServiceManager = null;
//	private Map<TestSession, Sites> commonSites = new HashMap<>(); // these are the common sites. The simulator based
	// sites are kept in Session.sites.
	private boolean alwaysReload = true;

	static Logger logger = Logger.getLogger(SiteServiceManager.class);

	private SiteServiceManager() {
	}

	static  SiteServiceManager getSiteServiceManager() {
		if (siteServiceManager == null)
			siteServiceManager = new SiteServiceManager();
		return siteServiceManager;
	}

	static  SiteServiceManager getInstance() { return getSiteServiceManager(); }

	// includes sims too
	 List<Site> getAllSites(String sessionId, TestSession testSession) throws Exception {
		logger.debug(sessionId + ": " + "getAllSites");

		Map<String, Site> sites = new HashMap<>();

		for (Site site : getCommonSites(TestSession.DEFAULT_TEST_SESSION).asCollection())
			sites.put(site.getName(), site);
		for (Site site : getCommonSites(testSession).asCollection())
			sites.put(site.getName(), site);

		List<SimId> simIds;
		simIds = SimDb.getAllSimIds(TestSession.DEFAULT_TEST_SESSION);
		for (SimId simId : simIds) {
			SimulatorConfig config = new SimDb().getSimulator(simId);
			if (config != null) {
				AbstractActorFactory af = AbstractActorFactory.getActorFactory(config);
				assert af : "No ActorFactory for " + config
				Site site = af.getActorSite(config, null);
				sites.put(site.getName(), site);
			}
		}
		simIds = SimDb.getAllSimIds(testSession);
		for (SimId simId : simIds) {
			SimulatorConfig config = new SimDb().getSimulator(simId);
			if (config!=null) {
				AbstractActorFactory af = AbstractActorFactory.getActorFactory(config);
				Site site = af.getActorSite(config, null);
				sites.put(site.getName(), site);
			}
		}

		List<Site> returns = new ArrayList<>();
		returns.addAll(sites.values());
		logger.debug("allSites are " + returns);
		return returns;

	}



	 List<String> getSiteNames(String sessionId, boolean reload, boolean returnSimAlso, TestSession testSession, boolean qualified)   {

			if (returnSimAlso) {  // implemented as return proxy ONLY???
				List<String> names = new ArrayList<>();
				for (Site s : getAllSites(sessionId, testSession))
					names.add((qualified)? getQualifiedName(s) : s.getName());
				return names;
			} else {
				Set<String> names = new HashSet<>();
				Sites sites = getCommonSites(testSession);
				if (sites != null) {
					for (Site s : sites.asCollection())
						names.add((qualified)? getQualifiedName(s) : s.getName());
				}
				if (!testSession.equals(TestSession.DEFAULT_TEST_SESSION)) {
					sites = getCommonSites(TestSession.DEFAULT_TEST_SESSION);
					if (sites != null) {
						for (Site s : sites.asCollection())
							names.add((qualified)? getQualifiedName(s) : s.getName());
					}
				}
				List<String> nameList = new ArrayList<>();
				nameList.addAll(names);
				return nameList;
			}
	}

	private String getQualifiedName(Site s) {
		TestSession owningTestSession = (s.getOwner() == null) ? TestSession.DEFAULT_TEST_SESSION : new TestSession(s.getOwner());
		if (Installation.instance().testSessionExists(owningTestSession) || owningTestSession.equals(TestSession.GAZELLE_TEST_SESSION))
			return owningTestSession.getValue() + ":" + s.getName();
		return "UNDEFINED:" + s.getName();
	}

	// continue to load from EC/actors base dir - otherwise IT tests are almost impossible to write
	private Sites load(TestSession testSession) throws Exception {
		File dir;
		dir = Installation.instance().actorsDir();
		Sites ss = new SeparateSiteLoader(testSession).load(dir, null);

		dir = Installation.instance().actorsDir(testSession);
		Sites ss2 = new SeparateSiteLoader(testSession).load(dir, null);
		ss.add(ss2);
		logger.debug("Loaded (" + testSession + "): " + ss.toString());
		return ss;
	}

	// Statically defined sites (does not include simulators)
	// how is the commonSites variable helping?
	 Sites getCommonSites(TestSession testSession) throws FactoryConfigurationError,
			Exception {
		logger.debug("getCommonSites(" + testSession + ")");

		Sites retSites = load(TestSession.DEFAULT_TEST_SESSION);
		if (!testSession.equals(TestSession.DEFAULT_TEST_SESSION))
			retSites.add(load(testSession));

		logger.debug("Returned common: " + retSites);
		return retSites;

	}

	private static boolean useActorsFile() {
		return false
	}

	 boolean useGazelleConfigFeed() {
		String c = Installation.instance().propertyServiceManager().getToolkitGazelleConfigURL();
		return c.trim().length() > 0;
	}

	private TestSession getTestSession(List<Site> sites) {
		for (Site s : sites) {
			if (!s.getTestSession().equals(TestSession.DEFAULT_TEST_SESSION))
				return s.getTestSession();
		}
		return TestSession.DEFAULT_TEST_SESSION;
	}

	private Sites asSites(List<Site> theSites) {
		TestSession testSession = getTestSession(theSites);
		Sites sites = new Sites(testSession);

		for (Site site : theSites) {
			sites.add(site);
		}

		return sites;
	}

	 List<String> reloadSites(String sessionId, boolean simAlso, TestSession testSession)
			throws FactoryConfigurationError, Exception {
		logger.debug(sessionId + ": " + "reloadSites");
		return getSiteNames(sessionId, true, simAlso, testSession, false);
	}


	 Site getSite(String sessionId, String siteName, TestSession testSession) throws Exception {
		logger.debug(sessionId + ": " + "getSite");
		try {
			try {
				return SimManager.getAllSites(testSession).getSite(siteName, testSession);
			} catch (Exception e) {
				return SimManager.getAllSites(TestSession.DEFAULT_TEST_SESSION).getSite(siteName, TestSession.DEFAULT_TEST_SESSION);
			}
		} catch (Exception e) {
			logger.error("getSite", e);
			throw new Exception(e.getMessage());
		}
	}


	 String saveSite(String sessionId, Site site, TestSession testSession)  {
		return null;
	}

	 String deleteSite(String sessionId, String siteName, TestSession testSession) throws Exception {
		return null;
	}

	 Site getSite(Sites allSites, String name) throws Exception {
		return getSite(allSites.getAllSites().asCollection(), name);
	}

	 Site getSite(Collection<Site> allSites, String name) throws Exception {

		for (Site s : allSites) {
			if (s.getName().equals(name))
				return s;
		}

		throw new Exception("Site [" + name + "] is not defined");
	}

	// don't return sites implemented via simulators
	 List<String> reloadCommonSites(TestSession testSession) throws FactoryConfigurationError,
			Exception {
		logger.debug("reloadCommonSites for " + testSession.getValue());
		Sites sites = getCommonSites(testSession);    // does reload
		List<String> values = sites.getSiteNames();
		logger.debug("reloaded CommonSites are " + testSession.getValue() + ": " + values);
		return values;
	}

	 List<String> getActorTypeNames(String sessionId) {
		logger.debug(sessionId + ": " + "getActorTypeNames");
		return gov.nist.asbestos.simapi.tk.actors.ActorType.getActorNamesForConfigurationDisplays();
	}

	 void promoteSiteToDefault(String siteName, TestSession testSession) throws Exception {
		SeparateSiteLoader srcLoader = new SeparateSiteLoader(testSession);
		File srcDir = Installation.instance().actorsDir(testSession);
		Sites sites = srcLoader.load(srcDir, new Sites(testSession));
		Site site = sites.getSite(siteName, testSession);

		site.setOwner(testSession.getValue());

		SeparateSiteLoader tgtLoader = new SeparateSiteLoader(TestSession.DEFAULT_TEST_SESSION);
		File tgtDir = Installation.instance().actorsDir(TestSession.DEFAULT_TEST_SESSION);
		tgtLoader.saveToFile(tgtDir, site);

		srcLoader.delete(srcDir, siteName);
	}

}
