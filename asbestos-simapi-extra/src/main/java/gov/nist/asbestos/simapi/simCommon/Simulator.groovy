package gov.nist.asbestos.simapi.simCommon;

import com.google.gwt.user.client.rpc.IsSerializable
import groovy.transform.TypeChecked

@TypeChecked
 class Simulator implements Serializable, IsSerializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 8914156242225793229L;
	List<SimulatorConfig> configs;

	 Simulator() {
		configs = new ArrayList<SimulatorConfig>();
	}

	 Simulator(List<SimulatorConfig> configs) {
		this.configs = configs;
	}

	 Simulator(SimulatorConfig config) {
		configs = new ArrayList<SimulatorConfig>();
		configs.add(config);
	}

	 List<SimulatorConfig> getConfigs() {
		return configs;
	}

	 int size() { return configs.size(); }

	 SimulatorConfig getConfig(int i) { return configs.get(i); }

	 List<SimId> getIds() {
		List<SimId> ids = new ArrayList<SimId>();
		for (SimulatorConfig c : configs)
			ids.add(c.id);
		return ids;
	}

	 String toString() {
		StringBuilder buf = new StringBuilder();

		for (SimulatorConfig conf : configs) buf.append(conf.toString()).append('\n');

		return buf.toString();
	}
}
