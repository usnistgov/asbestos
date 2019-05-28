package gov.nist.asbestos.simapi.simCommon;

import java.io.File;

import static gov.nist.asbestos.simapi.tk.installation.Installation.*;


/**
 * FHIR extension of SimDb
 */

class FSimDb {
    File fsimDb = instance().fsimDbFile();
    SimDb simDb;

    FSimDb() {
        simDb = new SimDb();
    }

    FSimDb(SimId simId) {
        simDb = new SimDb(simId);
    }

}
