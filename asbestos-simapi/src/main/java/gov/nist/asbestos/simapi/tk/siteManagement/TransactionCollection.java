package gov.nist.asbestos.simapi.tk.siteManagement;


import gov.nist.asbestos.simapi.tk.actors.ActorType;
import gov.nist.asbestos.simapi.tk.actors.TransactionType;

import java.util.ArrayList;
import java.util.List;

class TransactionCollection {
	 private List<TransactionBean> transactions = new ArrayList<TransactionBean>();
	 private String collectionName;    // never really used
	private boolean repositories = false; // a TransactionCollection is either for Repositories
									// or not

	 void mergeIn(TransactionCollection tc) {
		 transactions.addAll(tc.transactions);
	}

	 boolean equals(TransactionCollection tc) {
		if (tc == null)
			return false;
		return
				repositories == tc.repositories &&
				((collectionName == null) ? tc.collectionName == null : collectionName.equals(tc.collectionName)) &&
				transactionsEquals(tc.transactions);
	}

	boolean transactionsEquals(List<TransactionBean> t) {
		List<TransactionBean> t2 = new ArrayList<TransactionBean>(t);
		for (TransactionBean b : transactions) {
			int index = index(t2, b);
			if (index == -1)
				return false;
			t2.remove(index);
		}
		return t2.size() == 0;
	}

	int index(List<TransactionBean> tc, TransactionBean b) {
		if (tc == null || b == null)
			return -1;
		int i=0;
		for (TransactionBean b1 : tc) {
			if (b.equals(b1))
				return i;
			i++;
		}
		return -1;
	}

	 void fixTlsEndpoints() {
		for (TransactionBean transbean : transactions) {
			if (transbean.getEndpoint() == null || transbean.getEndpoint().equals(""))
				continue;
			if (transbean.isSecure()) {
				if (transbean.getEndpoint().startsWith("http:"))
					transbean.setEndpoint(transbean.getEndpoint().replaceFirst("http:", "https:"));
			} else {
				if (transbean.getEndpoint().startsWith("https:"))
					transbean.setEndpoint(transbean.getEndpoint().replaceFirst("https:", "http:"));
			}
		}
	}


//	 void removeEmptyEndpoints() {
//		List<TransactionBean> removable = new ArrayList<TransactionBean>();
//
//		for (TransactionBean transbean : transactions) {
//			if (transbean.endpoint == null || transbean.endpoint.trim().equals(""))
//				removable.add(transbean);
//		}
//		transactions.removeAll(removable);
//	}

	 void removeEmptyNames() {
		List<TransactionBean> removable = new ArrayList<TransactionBean>();

		for (TransactionBean transbean : transactions) {
			if (transbean.name == null || transbean.name.trim().equals(""))
				removable.add(transbean);
		}
		transactions.removeAll(removable);
	}

	boolean contains(TransactionBean b) {
		for (TransactionBean tb : transactions) {
			if (tb.hasSameIndex(b))
				return true;
		}
		return false;
	}

	 void addTransaction(TransactionBean transbean) {
		if (!contains(transbean))
			transactions.add(transbean);
	}

	static TransactionType getTransactionFromCode(String transactionCode) {
		return TransactionType.find(transactionCode);
	}

	static  String getTransactionName(String transactionCode) {
		TransactionType tt = TransactionType.find(transactionCode);
		if (tt == null)
			return "";
		return tt.getName();
	}

	static  List<ActorType> getActorTypes() {
		return ActorType.types;
	}

	static List<String> asList(String[] arry) {
		List<String> l = new ArrayList<String>();

		for (int i=0; i<arry.length; i++)
			l.add(arry[i]);

		return l;
	}

	 int size() {
		return transactions.size();
	}

	 TransactionCollection() {} // For GWT

	// instead of the boolean, subtypes should be used
	 TransactionCollection(boolean repositories) {
		this.repositories = repositories;
	}

	// Not used
	@Deprecated
    TransactionCollection(String collectionName) {
		transactions = new ArrayList<TransactionBean>();
		this.collectionName = collectionName;
	}

	@Deprecated
	 void setName(String name) {
		collectionName = name;
	}

	 boolean hasActor(gov.nist.asbestos.simapi.tk.actors.ActorType actor) {
		for (TransactionBean t : transactions) {
			if (!t.hasEndpoint())
				continue;
			try {
				if (actor.hasTransaction(t.getTransactionType()))
					return true;
			} catch (Exception e) {}
		}
		return false;
	}

	 TransactionBean find(gov.nist.asbestos.simapi.tk.actors.TransactionType transType, boolean isSecure, boolean isAsync) {
		return find(transType.getName(), isSecure, isAsync);
	}

	 TransactionBean find(String name, boolean isSecure, boolean isAsync) {
		if (name == null)
			return null;
		for (TransactionBean t : transactions) {
			if (t.hasName(name) &&
					isSecure == t.isSecure() &&
					isAsync == t.isAsync())
				return t;
		}
		return null;
	}

	 List<TransactionBean> findAll(String transactionName, boolean isSecure, boolean isAsync) {
		List<TransactionBean> tbs = new ArrayList<TransactionBean>();
		if (transactionName == null)
			return null;
		for (TransactionBean t : transactions) {
			if (t.hasName(transactionName) &&
					isSecure == t.isSecure() &&
					isAsync == t.isAsync())
				tbs.add(t);
		}
		return tbs;
	}

	 String get(TransactionType name, boolean isSecure, boolean isAsync) {
		TransactionBean t = find(name, isSecure, isAsync);
		if (t == null)
			return null;
		return t.getEndpoint();
	}

	 String get(String name, boolean isSecure, boolean isAsync) {
		TransactionBean t = find(name, isSecure, isAsync);
		if (t == null)
			return null;
		return t.getEndpoint();
	}

	 void add(String transactionName, String endpoint, boolean isSecure, boolean isAsync) throws Exception {
		TransactionBean t = find(transactionName, isSecure, isAsync);
		if (t != null)
			return;
//			throw new Exception("Actors.xml configuration problem: site " + collectionName +
//					" defines actor " + t.toString() + " multiple times\n Relevant part of Site definition is:\n" + toString());
		// Issue 98 TODO: set the repositoryType here
		transactions.add(new TransactionBean(
				transactionName,
				repositories ? TransactionBean.RepositoryType.REPOSITORY : TransactionBean.RepositoryType.NONE,
				endpoint,
				isSecure,
				isAsync));
	}

	 public String toString() {

		StringBuffer buf = new StringBuffer();

		buf.append("Collection Name: " + collectionName).append("\n");
		buf.append("Transactions:\n");
		for (TransactionBean t : transactions) {
			buf.append("\t");
			buf.append(t);
			buf.append("\n");
		}
		return buf.toString();
	}

     String describe() { return toString(); }

	public List<TransactionBean> getTransactions() {
		return transactions;
	}
}
