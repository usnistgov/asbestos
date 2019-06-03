package gov.nist.asbestos.simapi.tk.siteManagement;


import gov.nist.asbestos.simapi.simCommon.TestSession;

import java.util.*;

public class Sites {
	private HashMap<String, Site> siteMap = new HashMap<String, Site>();   // siteName -> Site
	private String defaultSiteName;
	private final  static String ALL_REPOSITORIES = "allRepositories";
	private final  static String FAKE_SITE_NAME = "fake_site";
	private final  static Site FAKE_SITE = new Site(FAKE_SITE_NAME, gov.nist.asbestos.simapi.simCommon.TestSession.DEFAULT_TEST_SESSION);
	private TestSession testSession = null;

	public boolean equals(Sites s) {
		if (s == null)
			return false;
		HashMap<String, Site> map1 = new HashMap<String, Site>(siteMap);
		HashMap<String, Site> map2 = new HashMap<String, Site>(s.siteMap);
		map1.remove(ALL_REPOSITORIES);
		map2.remove(ALL_REPOSITORIES);
		boolean mapped = equals(map1, map2);

		return ((defaultSiteName == null) ? s.defaultSiteName == null : defaultSiteName == s.defaultSiteName) && mapped;
	}

	 public boolean exists(String siteName) {
		return siteMap.keySet().contains(siteName);
	}

	 public int size() {
		return siteMap.size();
	}

	public boolean equals(HashMap<String, Site> map1, HashMap<String, Site> map2) {
		if (map1 == null)
			if (map2 == null)
				return true;
		if (map1 == null)
			return false;
		if (map2 == null)
			return false;
		if (map1.size() != map2.size())
			return false;
		for (String name : map1.keySet()) {
			if (!map2.containsKey(name))
				return false;
			if (!map1.get(name).equals(map2.get(name)))
				return false;
		}
		return true;
	}

	 public Sites clone() {
		Sites s = new Sites(testSession);
		s.siteMap = new HashMap<String, Site>();
		for (String key: siteMap.keySet()) {
			Site value = siteMap.get(key);
			s.siteMap.put(key, value);
		}
		s.defaultSiteName = defaultSiteName;

		return s;
	}

	 public HashMap<String, Site> getSiteMap() {
		return siteMap;
	}

	 public Sites getAllSites() {
		return new Sites(siteMap.values());
	}

	 public Collection<Site> asCollection() {
		return siteMap.values();
	}

	 public void validate(StringBuffer buf) {
		for (Site s : asCollection()) {
			s.validate(buf);
		}
	}

	 public Sites(TestSession testSession) {
		this.testSession = testSession;
	}

	 public Sites(Site site) {
//		if (!testSession.equals(site.getTestSession()))
//			throw new ToolkitRuntimeException("TestSession mismatch - trying to add " + site + " to Sites/" + testSession);
		siteMap.put(site.getName(), site);
		validate();
	}

	private TestSession testSessionFromCollection(Collection<Site> sites) {
		for (Site site : sites) {
			if (site.getTestSession() != null && !site.getTestSession().equals(TestSession.DEFAULT_TEST_SESSION))
				return site.getTestSession();
		}
		return gov.nist.asbestos.simapi.simCommon.TestSession.DEFAULT_TEST_SESSION;
	}

	 public Sites(Collection<Site> sites) {
		if (testSession == null && !sites.isEmpty())
			testSession = testSessionFromCollection(sites);
		for (Site site : sites)
			siteMap.put(site.getName(), site);
		validate();
	}

	 public void validate() {
		for (Site site : siteMap.values()) {
			site.validate();
			if (!isTestSessionOk(site))
				throw new RuntimeException("TestSession mismatch - Sites container is " + testSession + " but site is " + site.getTestSession());
		}
	}

	private boolean isTestSessionOk(Site site) {
		if (testSession == null) return false;
		if (site.getTestSession() == null) return false;
		if (site.getTestSession().equals(gov.nist.asbestos.simapi.simCommon.TestSession.DEFAULT_TEST_SESSION)) return true;
		if (site.getTestSession().equals(testSession)) return true;
		return false;
	}

	 public void add(Site site) {
		siteMap.put(site.getName(), site);
//		validate();
	}

	 public Sites add(Sites sites) {
		for (Site s : sites.siteMap.values())
			add(s);
//		validate();
		return this;
	}

	public void deleteSite(String siteName) {
		siteMap.remove(siteName);
	}

	public String getAllRepositoriesSiteName() { // This string is present in ActorConfigTab.java as well (client)
		return "allRepositories";
	}

	public Site getAllRepositoriesSite() {
		return siteMap.get(getAllRepositoriesSiteName());
	}

	// The special site "allRepositories" is created dynamically at run time
	// and holds the repositoryUniqueId => endpoint mappings for all
	// repositories in the configuration.  This allows any repository
	// to be targetted at a Connectathon. The GUI tool xdstools2 knows
	// about this special site.
	public void buildRepositoriesSite(gov.nist.asbestos.simapi.simCommon.TestSession testSession) {
		Site repSite = new Site(getAllRepositoriesSiteName(), testSession);
		TransactionCollection repSiteReps = repSite.repositories();
		for (String siteName : siteMap.keySet()) {
			Site site = siteMap.get(siteName);
			TransactionCollection reps = site.repositories();
			for (TransactionBean t : reps.getTransactions() ) {
				repSiteReps.getTransactions().add(t);
			}
		}
		siteMap.put(getAllRepositoriesSiteName(), repSite);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(testSession).append(": [");
		boolean first = true;
		for (Site site : siteMap.values()) {
			if (!first) buf.append(", ");
			first = false;
			buf.append(site.getFullName());
		}
		buf.append("]");

		return buf.toString();
	}

	public List<Site> getSitesWithActor(gov.nist.asbestos.simapi.tk.actors.ActorType actorType, gov.nist.asbestos.simapi.simCommon.TestSession testSession) {
		List<Site> rs = new ArrayList<Site>();

		for (String siteName : siteMap.keySet()) {
			Site site = getSite(siteName, testSession);
			if (site.hasActor(actorType))
				rs.add(site);
		}

		return rs;

	}

//	 List<Site> getSitesWithTransaction(TransactionType tt, TestSession testSession) throws Exception {
//		List<Site> rs = new ArrayList<Site>();
//
//		for (String siteName : siteMap.keySet()) {
//			Site site = getSite(siteName, testSession);
//			if (site.hasTransaction(tt))
//				rs.add(site);
//		}
//
//		return rs;
//
//	}

	public Site getSiteForHome(String home) {
		for (Site site : siteMap.values()) {
			if (home.equals(site.getHome()))
				return site;
		}
		return null;
	}

   /**
    * @param uid unique id to look for.
    * @param repType type of repository to search
    * @return Site instance with that id, or null if not found.
    */
   public Site getSiteForRepUid(String uid, TransactionBean.RepositoryType repType) {
      for (Site site : siteMap.values()) {
         if (site.transactionBeanForRepositoryUniqueId(uid, repType) != null)
            return site;
      }
      return null;
   }

	public  List<String> getSiteNamesWithActor(gov.nist.asbestos.simapi.tk.actors.ActorType actorType, gov.nist.asbestos.simapi.simCommon.TestSession testSession) throws Exception {
		List<String> rs = new ArrayList<String>();

		for (Site s : getSitesWithActor(actorType, testSession)) {
			rs.add(s.getName());
		}

		return rs;
	}

//	 List<String> getSiteNamesWithTransaction(TransactionType tt, TestSession testSession) throws Exception {
//		List<String> rs = new ArrayList<String>();
//
//		for (Site s : getSitesWithTransaction(tt, testSession)) {
//			rs.add(s.getName());
//		}
//
//		return rs;
//	}

	public List<String> getSiteNamesWithRepository(gov.nist.asbestos.simapi.simCommon.TestSession testSession) throws Exception {
		List<String> rs = new ArrayList<String>();

		for (String siteName : siteMap.keySet()) {
			Site site = getSite(siteName, testSession);
			if (site.hasRepositoryB())
				rs.add(siteName);
		}

		return rs;
	}

	public List<String> getSiteNames() {
		if (siteMap != null) {
			Set<String> set = siteMap.keySet();
			List<String> lst = new ArrayList<>(set);
			return lst;
		}
		return new ArrayList<String>();
	}

	public  Site getDefaultSite(TestSession testSession) throws Exception {
		if (defaultSiteName == null || defaultSiteName.equals(""))
			throw new Exception("No Default Site");
		return getSite(defaultSiteName, testSession);
	}

	public Site getSite(String siteName, TestSession testSession) {
   		if (siteName.equals(FAKE_SITE_NAME))
   			return FAKE_SITE;
		if (siteName == null)
			throw new RuntimeException("Internal error: null site requested");
		if (siteName.equals("gov/nist/toolkit/installation/shared"))
			return new Site("gov/nist/toolkit/installation/shared", testSession);
		List<String> sitenames = getSiteNames();
		if ( !sitenames.contains(siteName)) {
			if (sitenames.contains(testSession.getValue() + "__" + siteName))
				siteName = testSession.getValue() + "__" + siteName;
			else if (!sitenames.contains(siteName))
				throw new RuntimeException("Site [" + siteName + "] is not defined");
		}
		Site s = siteMap.get(siteName);
		return s;
	}

	public void setSites(HashMap<String, Site> sites) {
		this.siteMap = sites;
		validate();
	}

	public void setDefaultSite(String name) {
		defaultSiteName = name;
	}


	public void putSite(Site s) {
        String name = s.getName();
		siteMap.put(name, s);
	}

	public boolean isEmpty(String x) {
		if (x == null)
			return true;
		if (x.equals(""))
			return true;
		return false;
	}

//	 Sites getSites(Collection<SimulatorConfig> simConfigs) throws Exception {
//		Collection<Site> origSites = asCollection();
//		Collection<Site> scs = new ArrayList<Site>();
//		scs.addAll(origSites);
//		scs.addAll(SimManager.getSites(simConfigs));
//		return new Sites(scs);
//	}

	public HashMap<String, Site> getSites() {
		return siteMap;
	}

	public String getDefaultSiteName() {
		return defaultSiteName;
	}

	/**
	 * Site may be linked by orchestration.  If it is return according to documentation in Site.java
	 * @param siteSpec
	 * @return
	 */
	public Site getOrchestrationLinkedSites(SiteSpec siteSpec) throws Exception {
		if (siteSpec.getOrchestrationSiteName() != null) {
			Site oSite = getSite(siteSpec.getOrchestrationSiteName(), siteSpec.getTestSession());
			Site sutSite = getSite(siteSpec.getName(), siteSpec.getTestSession());
			oSite.addLinkedSite(sutSite);
			return oSite;
		}

		return getSite(siteSpec.getName(), siteSpec.getTestSession());
	}

}
