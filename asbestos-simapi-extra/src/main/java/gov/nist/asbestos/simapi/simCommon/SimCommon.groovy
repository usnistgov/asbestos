package gov.nist.asbestos.simapi.simCommon


import groovy.transform.TypeChecked;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * All simulators are passed an instance of this class in the constructor giving
 * the simulator access to all the necessary goodies.
 * @author bill
 *
 */

// NOTE: This should be limited to supporting functions for all simulators.
    // References to Regindex, RepIndex, RegistryErrorListGenerator, documentsToAttach should be refactored out
@TypeChecked
 class SimCommon {
	 SimDb db = null;
	boolean tls = false;
	 gov.nist.asbestos.simapi.tk.stubs.ValidationContext vc = null;
	 SimulatorConfig simConfig = null;
	 HttpServletRequest request = null;
	 HttpServletResponse response = null;
	 OutputStream os = null;
	 boolean faultReturned = false;
	 boolean responseSent = false;
	private static Logger logger = Logger.getLogger(SimCommon.class);
	 gov.nist.asbestos.simapi.tk.stubs.MessageValidatorEngine mvc;
	 gov.nist.asbestos.simapi.tk.actors.TransactionType transactionType;
	 gov.nist.asbestos.simapi.tk.actors.ActorType actorType;

	 boolean isResponseSent() {
		return faultReturned || responseSent;
	}

    SimCommon(SimDb db, SimulatorConfig simConfig, boolean tls, gov.nist.asbestos.simapi.tk.stubs.ValidationContext vc,
              HttpServletRequest request, HttpServletResponse response, gov.nist.asbestos.simapi.tk.stubs.MessageValidatorEngine mvc) {
      this(db, tls, vc, response, mvc);
      this.simConfig = simConfig;
      this.request = request;
      this.mvc = mvc;
   }


	/**
	 * Build a new simulator support model
	 * @param db the simulator database model supporting this simulator
	 * @param tls is tls employed
	 * @param vc validation context used to validate the input message
	 * @param response HttpServletResponse model for accepting eventual output
	 */
	 SimCommon(SimDb db, boolean tls, gov.nist.asbestos.simapi.tk.stubs.ValidationContext vc, HttpServletResponse response, gov.nist.asbestos.simapi.tk.stubs.MessageValidatorEngine mvc)  {

		this.db = db;
		this.tls = tls;
		this.vc = vc;
		this.response = response;
		this.mvc = mvc;
		if (response != null)
			this.os = response.getOutputStream();
	}

	/**
     * Used only to issue soap faults, don't have enough context to do more
     * @param response
     * @throws IOException
     */

     SimCommon(HttpServletResponse response) throws IOException {
        this.response = response;
        if (response != null)
            os = response.getOutputStream();
    }

	/**
	 * Is TLS enabled?
	 * @return
	 */
	 boolean isTls() { return tls; }

	/**
	 * Return current validation context.
	 * @return
	 */
	 gov.nist.asbestos.simapi.tk.stubs.ValidationContext getValidationContext() {
		return vc;
	}

	 void setValidationContext(gov.nist.asbestos.simapi.tk.stubs.ValidationContext vc) {
		this.vc = vc;
	}

	static  void deleteSim(SimId simulatorId) {
		try {
			logger.info("Delete proxy " + simulatorId);
			SimDb simdb = new SimDb(simulatorId);
			File simdir = simdb.getIpDir();
			simdir.delete()
		} catch (Exception e) {
			// doesn't exist - ok
		}
	}

	 void setLogger(Logger log) {
	   logger = log;
	}

	 void sendHttpFault(String em) {
	   sendHttpFault(400, em);
	}
	 void sendHttpFault(int status, String em) {
	   logger.info("HttpPost Error response: " + status + " " + em);
	   try {
         response.sendError(status, em);
      } catch (IOException e) {
         logger.warn("IO error sending http response");
      }
	}

	 gov.nist.asbestos.simapi.tk.actors.TransactionType getTransactionType() {
		return transactionType;
	}

	 void setTransactionType(gov.nist.asbestos.simapi.tk.actors.TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	 gov.nist.asbestos.simapi.tk.actors.ActorType getActorType() {
		return actorType;
	}

	 void setActorType(gov.nist.asbestos.simapi.tk.actors.ActorType actorType) {
		this.actorType = actorType;
	}
}
