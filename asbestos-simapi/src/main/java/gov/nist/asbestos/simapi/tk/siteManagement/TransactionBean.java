package gov.nist.asbestos.simapi.tk.siteManagement;


import gov.nist.asbestos.simapi.tk.actors.ActorType;
import gov.nist.asbestos.simapi.tk.actors.TransactionType;

/**
 * Configuration of a single instance of a actor. Transactions are
 * split into two major types: Retrieve and All Others. The isRetrieve()
 * determines which kind this is.  In theory a actor must belong to an
 * Actor definition.  Toolkit is a bit looser and transactions belong
 * to TransactionOfferings which can be thought of as a lightweight
 * definition of an Actor.
 * @author bill
 *
 */

 public class TransactionBean {
	 private boolean secure = false;
	 private boolean async = false;
	 private String endpoint = "";

	String name = "";   // can be actor name or repository uid
						// when a actor name, it is related to transType
	private TransactionType transType = null;
	private ActorType actorType = null;


	// REMOVE this? Not used for anything real yet.
	 enum RepositoryType { REPOSITORY, ODDS, NONE, IDS;

		RepositoryType() {}
	};

	 RepositoryType repositoryType;

	 String getEndpoint() { return endpoint; }
	 void setEndpoint(String endpoint) { this.endpoint = endpoint; }

	 boolean hasSameIndex(TransactionBean b) {
		return
				secure == b.secure &&
				async == b.async &&
				((name == null) ? b.name == null : name.equals(b.name)) &&
				((transType == null) ? b.transType == null : transType == b.transType) &&
				((actorType == null) ? b.actorType == null : actorType == b.actorType) &&
				((repositoryType == null) ? b.repositoryType == null : repositoryType == b.repositoryType);
	}

	 boolean equals(TransactionBean b) {
		return hasSameIndex(b) &&
				(
						("".equals(endpoint) && "".equals(b.endpoint)) ||
						endpoint.equals(b.endpoint)
						);
	}

	 boolean hasName(String nam) {
		if (name.equals(nam))
			return true;
		if (transType == null)
			return false;
		if (transType.getName().equals(nam))
			return true;
		if (transType.getShortName().equals(nam))
			return true;
		return false;
	}

	 String getName() {
		if (transType == null)
			return name;
		return transType.getName();
	}

	@Override
    public String toString() {
		if (transType != null)
			return "[trans=" + transType +
//					" RepositoryType=" + repositoryType +
					" ActorType=" + (actorType == null ? "?" : actorType.getName()) +
					" secure=" + secure + " async=" + async + "] : " + endpoint;
		return "[repositoryUniqueId=" + name + " secure=" + secure + " async=" + async + "] : " + endpoint;
	}

	 boolean isRetrieve() {
		return isNameUid();
	}

	 boolean isNameUid() {
		if (name == null || name.equals(""))
			return false;
		return Character.isDigit(name.charAt(0));
	}

	 gov.nist.asbestos.simapi.tk.actors.TransactionType getTransactionType() {
		return transType;
	}

	 boolean isType(gov.nist.asbestos.simapi.tk.actors.TransactionType transType2) {
		try {
			return transType2.equals(transType);
		} catch (Exception e) {
			return false;
		}
	}

	 boolean hasEndpoint() {
		return endpoint != null && !endpoint.equals("");
	}

	 TransactionBean() {

	}

	// Used by simulator factories, ActorConfigTab and the Gazelle interface
	 TransactionBean(String name, RepositoryType repositoryType, String endpoint, boolean secure, boolean async) {
		this.name = name;  // comes from TransactionType.XXX.getCode()
							// param should be TransactionType
							// This constructor should be retired in favor of the next one which depends on TransactionType

		// name can be trans name or repository uid
		transType = gov.nist.asbestos.simapi.tk.actors.TransactionType.find(name);
		this.repositoryType = repositoryType;
		this.endpoint = endpoint;
		this.secure = secure;
		this.async = async;
	}

	// Used only by ActorConfigTab
	 TransactionBean(gov.nist.asbestos.simapi.tk.actors.TransactionType transType, RepositoryType repositoryType, String endpoint, boolean secure, boolean async) {
		this.transType = transType;
		this.name = transType.getName();
		this.repositoryType = repositoryType;
		this.endpoint = endpoint;
		this.secure = secure;
		this.async = async;
	}

	// Used only by Gazelle interface
	@Deprecated
	 TransactionBean(gov.nist.asbestos.simapi.tk.actors.TransactionType transType, RepositoryType repositoryType, gov.nist.asbestos.simapi.tk.actors.ActorType actorType, String endpoint, boolean secure, boolean async) {
		this.transType = transType;
		this.name = transType.getName();
		this.repositoryType = repositoryType;
		this.actorType = actorType;
		this.endpoint = endpoint;
		this.secure = secure;
		this.async = async;
	}

	 void setName(String name) {
		this.name = name;
	}

	public boolean isSecure() {
		return secure;
	}

	public boolean isAsync() {
		return async;
	}

	public RepositoryType getRepositoryType() {
		return repositoryType;
	}

	public ActorType getActorType() {
		return actorType;
	}
}
