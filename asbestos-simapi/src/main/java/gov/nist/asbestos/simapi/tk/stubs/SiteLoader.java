package gov.nist.asbestos.simapi.tk.stubs;



import gov.nist.asbestos.simapi.simCommon.TestSession;
import gov.nist.asbestos.simapi.tk.siteManagement.Site;
import gov.nist.asbestos.simapi.tk.siteManagement.TransactionBean;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Objects;

public abstract class SiteLoader {
	TestSession testSession;

	HashMap<String, Site> siteMap = new HashMap<String, Site>();

	 public SiteLoader(TestSession testSession) {
		 Objects.requireNonNull(testSession);
		this.testSession = testSession;
	}

	public Site parseSite(OMElement ele) throws Exception {
		String site_name = ele.getAttributeValue(new QName("name"));
		if (site_name == null || site_name.equals(""))
			throw new Exception("Cannot parse Site with empty name from actors config file");
//		if (sites.containsKey(site_name))
//			throw new Exception("Site " + site_name + " is multiply defined in configuration file");
		Site site = new Site(site_name, testSession);
		parseSite(site, ele);
		putSite(site);
		return site;
	}

	//@SuppressWarnings("unchecked")
	protected
	void parseSite(Site s, OMElement conf) throws Exception {
	}

	 OMElement siteToXML(Site s) {
		 return null;
	}

	public void addTransactionXML(OMElement site_ele, TransactionBean tb) {
	}


	protected void putSite(Site s) {
	}

	protected String withoutSuffix(String inp, String suffix) {
		if (inp.endsWith(suffix))
			return inp.substring(0, inp.length() - suffix.length());
		return inp;
	}

}
