package gov.nist.asbestos.simapi.tk.stubs;


import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.siteManagement.Site;
import gov.nist.asbestos.simapi.tk.siteManagement.Sites;
import org.apache.axiom.om.OMElement;

import java.io.File;

public class SeparateSiteLoader extends SiteLoader {

	public SeparateSiteLoader(TestSession testSession) {
		super(testSession);
	}

	public Sites load(OMElement conf, Sites sites) throws Exception {
		parseSite(conf);

		if (sites == null)
			sites = new Sites(testSession);

		sites.setSites(siteMap);
		sites.buildRepositoriesSite(testSession);

		return sites;
	}

	 public Sites load(File actorsDir, Sites sites) throws Exception {
		 return null;
	}

	 public void saveToFile(File actorsDir, Sites sites) throws Exception {
		for (Site s : sites.asCollection()) {
			saveToFile(actorsDir, s);
		}
	}

	 public void saveToFile(File actorsDir, Site site) throws Exception {
	}

	 public void delete(File actorsDir, String siteName) {
	}

}
