package gov.nist.asbestos.simapi.simCommon;


import gov.nist.asbestos.simapi.tk.actors.TransactionType;
import gov.nist.asbestos.simapi.tk.siteManagement.Site;

import java.util.List;


/**
 * This class exists only to expose methods in ActorFactory such as
 * the buildNewSimulator method which
 * would otherwise be buried in the abstract class ActorFactory.
 * @author bill
 *
 */
public class GenericSimulatorFactory extends AbstractActorFactory {
	SimId newID = null;
	//@SuppressWarnings("unused")
	 GenericSimulatorFactory() {
	}

	 GenericSimulatorFactory(SimManager simManager) {
		super();
	}

	// ActorFactory needs to be directly
	 Simulator buildNewSimulator(SimManager simm, String simtype, SimId simID, String environment) throws Exception {
		return buildNewSimulator(simm, simtype, simID, environment, true);
	}

	@Override
	protected Simulator buildNew(SimManager simm, SimId newID, String environment, boolean configureBase)  {
		this.newID = newID;
		return null;
	}


	@Override
	protected void verifyActorConfigurationOptions(SimulatorConfig config) {

	}

	@Override
	Site buildActorSite(SimulatorConfig asc, Site site) {
		return null;
	}

	@Override
	List<TransactionType> getIncomingTransactions() {
		return null;
	}


}
