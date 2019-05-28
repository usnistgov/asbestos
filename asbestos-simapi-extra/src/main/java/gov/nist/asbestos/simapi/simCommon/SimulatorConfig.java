package gov.nist.asbestos.simapi.simCommon;


import java.util.*;

/**
 * Definition for an actor simulator.
 * @author bill
 *
 */
public class SimulatorConfig {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Globally unique id for this simulator
	 */
	SimId id;
	private String actorType;
	private Date expires;
	private boolean expired = false;
	private String environmentName = null;
	private List<SimulatorConfigElement> elements  = new ArrayList<SimulatorConfigElement>();

     boolean isExpired() { return expired; }
	 void isExpired(boolean is) { expired = is; }

	 boolean checkExpiration() {
		Date now = new Date();
		if (now.after(expires))
			expired = true;
		else
			expired = false;
		return expired;
	}

	// not sure what to do with the other attributes, leave alone for now
	 public void add(SimulatorConfig asc) {
		for (SimulatorConfigElement ele : asc.elements) {
			if (getFixedByName(ele.getName()) == null)
				elements.add(ele);
		}
	}

	 public String toString() {
		return id.toString();
	}

	 public SimulatorConfig(SimId id, String actorType, Date expiration, String environment) {
		this.id = id;
		this.actorType = actorType;
		expires = expiration;
		this.environmentName = environment;

		gov.nist.asbestos.simapi.tk.actors.ActorType at = gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(actorType);
		if (at != null && at.isFhir())
			this.id.forFhir();
	}

	 public List<SimulatorConfigElement> elements() {
		return elements;
	}

	 public void add(List<SimulatorConfigElement> elementList) {
		elements.addAll(elementList);
	}
     public void add(SimulatorConfigElement ele) { elements.add(ele); }

	 public Date getExpiration() {
		return expires;
	}

	 public List<SimulatorConfigElement> getFixed() {
		List<SimulatorConfigElement> fixed = new ArrayList<SimulatorConfigElement>();
		for (SimulatorConfigElement ele : elements) {
			if (!ele.isEditable())
				fixed.add(ele);
		}
		return fixed;
	}

	 public List<SimulatorConfigElement> getElements() { return elements; }

	 List<SimulatorConfigElement> getUser() {
		List<SimulatorConfigElement> user = new ArrayList<SimulatorConfigElement>();
		for (SimulatorConfigElement ele : elements) {
			if (ele.isEditable())
				user.add(ele);
		}
		return user;
	}

	public  SimulatorConfigElement	getUserByName(String name) {
		if (name == null)
			return null;

		for (SimulatorConfigElement ele : elements) {
			if (name.equals(ele.getName()))
				return ele;
		}
		return null;
	}

	public  SimulatorConfigElement	getFixedByName(String name) {
		if (name == null)
			return null;

		for (SimulatorConfigElement ele : elements) {
			if (name.equals(ele.getName()))
				return ele;
		}
		return null;
	}

	public SimulatorConfigElement getConfigEle(String name) {
        if (name == null)
            return null;

        for (SimulatorConfigElement ele : elements) {
            if (name.equals(ele.getName()))
                return ele;
        }
        return null;
    }

	public List<SimulatorConfigElement> getEndpointConfigs() {
		List<SimulatorConfigElement> configs = new ArrayList<>();

		for (SimulatorConfigElement config : elements) {
			if (config.getType() == ParamType.ENDPOINT) {
				configs.add(config);
			}
		}

		return configs;
	}

	public void deleteFixedByName(String name) {
		SimulatorConfigElement ele = getFixedByName(name);
		if (ele != null)
			elements.remove(ele);
	}

	public void deleteUserByName(String name) {
		SimulatorConfigElement ele = getUserByName(name);
		if (ele != null)
			elements.remove(ele);
	}

	public boolean hasConfig(String name) {
        return getFixedByName(name) != null;
    }

    /**
     * Removes configuration parameter with same name (if found) and adds
     * passed parameter.
    * @param replacement parameter to add/replace
    * @return true if existing parameter was replaced. false if no such
    * parameter was found, and passed parameter was added.
    */
	public boolean replace(SimulatorConfigElement replacement) {
       boolean replaced = false;
       Iterator<SimulatorConfigElement> itr = elements.iterator();
       while(itr.hasNext()) {
          SimulatorConfigElement existing = itr.next();
          if (existing.getName().equals(replacement.getName())) {
             itr.remove();
             replaced = true;
             break;
          }
       }
       elements.add(replacement);
       return replaced;
    }


	public SimId getId() {
		return id;
	}
	public  void setId(SimId simId) { id = simId; }

	public  String getActorType() {
		return actorType;
	}
	public  void setActorType(String type) { actorType = type; }

	public String actorTypeFullName() {
        String actorTypeName = getActorType();
        gov.nist.asbestos.simapi.tk.actors.ActorType type = gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(actorTypeName);
        if (type == null) return actorTypeName;
        return type.getName();
    }

	public SimulatorConfigElement get(String name) {
		for (SimulatorConfigElement ele : elements) {
			if (ele.getName().equals(name))
				return ele;
		}
		return null;
	}

	public String getDefaultName() {
		return get("Name").asString(); // + "." + getActorType();
	}

	public  String getEndpoint(gov.nist.asbestos.simapi.tk.actors.TransactionType transactionType) {
   		List<SimulatorConfigElement> transEles = getEndpointConfigs();
   		for (SimulatorConfigElement ele : transEles) {
   			if (ele.getTransactionType() == transactionType)
   				return ele.asString();
		}
		return null;
	}

	public TestSession getTestSession() {
		return id.getTestSession();
	}

	public String getEnvironmentName() { return environmentName; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimulatorConfig that = (SimulatorConfig) o;
		return expired == that.expired &&
				Objects.equals(id, that.id) &&
				Objects.equals(actorType, that.actorType) &&
				Objects.equals(expires, that.expires) &&
				Objects.equals(environmentName, that.environmentName) &&
				Objects.equals(elements, that.elements);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, actorType, expires, expired, environmentName, elements);
	}
}
