package gov.nist.asbestos.simapi.simCommon


import gov.nist.asbestos.simapi.tk.siteManagement.Site
import gov.nist.asbestos.simapi.tk.siteManagement.Sites;
import groovy.transform.TypeChecked;
import org.apache.log4j.Logger;


/**
 * Maintains the list of loaded SimulatorConfig objects for a
 * single session. This is obsolete
 *
 * Mostly this is a few static functions and a reference to the sessionId
 *
 * All methods that reference simConfigs are labeled @Synchronized
 * @author bill
 *
 */

@TypeChecked
 class SimManager {
	private String sessionId;  // this is never used internally.  Other classes use it through the getter.
	private static Logger logger = Logger.getLogger(SimManager.class);


	 SimManager(String sessionId) {
		this.sessionId = sessionId;
	}

//*****************************************
//  These methods would normally belong in class SimulatorConfig but that
//  class is compiled for the client and some of these classes (ActorFactory)
//	do not belong on the client side.
//*****************************************
	static  Site getSite(SimulatorConfig config) {
		AbstractActorFactory af = getActorFactory(config);
//        logger.info("Getting original actor factory to generate site - " + af.getClass().getName());
		Site site = af.getActorSite(config, null);
		assert site : "Simulator " + config.getId() + "(type " + af.getClass().getName() + ") threw error when asked to generate site object"
		return site.setSimulator(true);
	}

	static private AbstractActorFactory getActorFactory(SimulatorConfig config) {
		String simtype = config.getActorType();
		gov.nist.asbestos.simapi.tk.actors.ActorType at = gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(simtype);
		AbstractActorFactory af = new GenericSimulatorFactory().getActorFactory(at);
		return af;
	}

	static  Site getSite(SimId simId)  {
		SimulatorConfig config = new SimDb().getSimulator(simId);
		assert config : "Simulator " + simId.toString() + " does not exist"
		return getSite(config);
	}

//*****************************************

	 String sessionId() {
		return sessionId;
	}

	/**
	 * Get common sites and proxy sites defined for this session.
	 * @return
	 * @throws Exception
	 */
	static  Sites getAllSites(TestSession testSession) throws Exception {
		return getAllSites(SiteServiceManager.getSiteServiceManager().getCommonSites(testSession), testSession);
	}

	static  Sites getAllSites(Sites commonSites, TestSession testSession)  throws Exception {
		Sites sites;

		List<SimId> simIds = SimDb.getAllSimIds(testSession);

		if (commonSites == null)
			sites = new Sites(testSession);
		else
			sites = commonSites.clone();

		for (SimId simId : simIds) {
			try {
				Site site = getSite(simId);
				if (site != null)
					sites.putSite(site);
			} catch (Throwable nse) {
			}
		}

		sites.buildRepositoriesSite(testSession);

		return sites;
	}

	 boolean exists(String siteName, TestSession testSession) {
		try {
			if (siteName.equals("gov/nist/toolkit/installation/shared")) return true;
			if (SiteServiceManager.getSiteServiceManager().getCommonSites(testSession).exists(siteName)) return true;
			for (SimId simId : SimDb.getAllSimIds(testSession)) {
				if (siteName.equals(simId.toString())) return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

     List<Site> getSites(List<String> siteNames, TestSession testSession) throws Exception {
        List<Site> siteList = new ArrayList<>();

        Collection<Site> sites = getAllSites(testSession).asCollection();
        for (Site site : sites) {
            if (siteNames.contains(site.getName()))
                siteList.add(site);
        }

        return siteList;
    }
}
