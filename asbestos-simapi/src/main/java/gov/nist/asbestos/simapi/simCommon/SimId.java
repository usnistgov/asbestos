package gov.nist.asbestos.simapi.simCommon;


import gov.nist.asbestos.simapi.tk.actors.ActorType;
import gov.nist.asbestos.simapi.tk.siteManagement.SiteSpec;

import java.util.Objects;

/**
 *
 */
public class SimId {

    private static final String SEPARATOR = "__";
    private static final String SLASH = "/";

    private TestSession testSession = null;
    private String _id = null;
    private String actorType = null;
    private String environmentName = null;
    private boolean fhir = false;

    // server only
    public  SimId(TestSession testSession, String id, String actorType, String environmentName, boolean fhir)  {
        this(testSession, id, actorType);
        this.environmentName = environmentName;
        this.fhir = fhir;
    }

    // server only
     public SimId(TestSession testSession, String id, String actorType, String environmentName) {
        this(testSession, id, actorType);
        this.environmentName = environmentName;
    }

    // client only
     public SimId(TestSession testSession, String id, String actorType)  {
        this(testSession, id);
        this.actorType = actorType;
    }

     public SimId(SiteSpec siteSpec, TestSession testSession) {
        this(testSession, (siteSpec == null) ? null : siteSpec.getName());
        if (siteSpec != null && siteSpec.getActorType() != null)
            actorType = siteSpec.getTypeName();
    }

     public SimId(TestSession testSession, String id) {
         Objects.requireNonNull(testSession);
         Objects.requireNonNull(id);

        build(testSession, id);
    }

    public static SimId buildFromRawId(String rawid) {
         Objects.requireNonNull(rawid);
        if (!rawid.contains("__"))
            throw new RuntimeException("buildFromRawId: SimId is testsession__id : " + rawid + " is not valid format");
        String[] parts = rawid.split("__");
        String testSession = parts[0];
        String id = parts[1];
        return new SimId(new TestSession(testSession), id);
    }

    SimId withActorType(String actor) {
        this.actorType = actor;
        return this;
    }

    SimId withEnvironment(String environment) {
        this.environmentName = environment;
        return this;
    }

     SimId() {}

     SimId forFhir() {
        fhir = true;
        return this;
    }

     boolean isFhir() { return fhir; }

    private void build(TestSession testSession, String id) {
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(id);
        testSession.clean();
        if (testSession.getValue().contains(SEPARATOR))
            throw new RuntimeException(SEPARATOR + " is illegal in simulator testSession name");
        if (testSession.getValue().contains(SLASH))
            throw new RuntimeException(SLASH + " is illegal in simulator testSession name");

        id = cleanId(id);
        if (id.contains(SLASH))
            throw new RuntimeException(SLASH + " is illegal in simulator id");

        this.testSession = testSession;
        this._id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimId simId = (SimId) o;
        return fhir == simId.fhir &&
                Objects.equals(testSession, simId.testSession) &&
                Objects.equals(_id, simId._id) &&
                Objects.equals(actorType, simId.actorType) &&
                Objects.equals(environmentName, simId.environmentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testSession, _id, actorType, environmentName, fhir);
    }

    public String toString() { return testSession.toString() + SEPARATOR + _id; }

    public void validate() {
         String reason = validateState();
         if (reason != null)
             throw new RuntimeException(reason);

    }

    public String validateState() {
        StringBuilder buf = new StringBuilder();

        if (getTestSession() == null || getTestSession().toString().equals("")) buf.append("No testSession specified\n");
        if (getId() == null || getId().isEmpty()) buf.append("No id specified\n");
        if (actorType == null || actorType.equals("")) buf.append("No actorType specified\n");
        if (environmentName == null || environmentName.equals("")) buf.append("No environmentName specified");

        if (buf.length() == 0) return null;   // no errors
        return buf.toString();
    }

    public static String cleanId(String id) { return id.replaceAll("\\.", "_").toLowerCase(); }

     public String getActorType() {
        return actorType;
    }

     public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public TestSession getTestSession() {
        return testSession;
    }

    public String getId() {
        return _id;
    }

    public boolean isTestSession(TestSession testSession) {
        return testSession != null && testSession.equals(this.testSession);
    }
    public boolean isValid() { return (!isEmpty(testSession.getValue())) && (!isEmpty(getId())); }
    public void setValid(boolean x) { }
    private boolean isEmpty(String x) { return x == null || x.trim().equals(""); }

    public SiteSpec getSiteSpec() {
        SiteSpec siteSpec = new SiteSpec(testSession);
        siteSpec.setName(toString());
        if (actorType != null)
            siteSpec.setActorType(ActorType.findActor(actorType));
        return siteSpec;
    }

}
