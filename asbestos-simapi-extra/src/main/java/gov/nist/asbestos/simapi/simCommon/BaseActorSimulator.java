package gov.nist.asbestos.simapi.simCommon;


abstract public class BaseActorSimulator {
    public SimDb  db;
    public SimCommon common;
    public SimulatorConfig config;

    abstract public boolean run(gov.nist.asbestos.simapi.tk.actors.TransactionType transactionType, gov.nist.asbestos.simapi.tk.stubs.MessageValidatorEngine mvc, String validation);

    public BaseActorSimulator() {}

    public BaseActorSimulator(SimCommon simCommon) {
        this.common = simCommon;
        db = simCommon.getDb();
    }

    public void init(SimulatorConfig config) {
        this.config = config;
    }

    // Services may need extension via hooks.  These are the hooks
    // They are meant to be overloaded
    public void onCreate(SimulatorConfig config) {}
    public void onDelete(SimulatorConfig config) {}

    public void onTransactionBegin(SimulatorConfig config) {}
    public void onTransactionEnd(SimulatorConfig config) {}

    // simulatorConfig guaranteed to be initialized
    public void onServiceStart(SimulatorConfig config) {}  // these two refer to Servlet start/stop
    public void onServiceStop(SimulatorConfig config) {}

}
