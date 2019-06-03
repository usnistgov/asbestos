package gov.nist.asbestos.simapi.tk.actors;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


// This file must be kept up to date with SimulatorActorTypes.java

/**
 * Actor types defined by test engine.  A subset of these are available as simulators.
 *
 * The profile/actor/option are now coded in the following way.
 * Test collections.txt uses the actor(profile)_option format.
 * actorCode should match the actor code used in the actor part of the test collections.txt file.
 * profile should match the profile part of the test collections.txt file. If the profile is not specified, XDS is assumed, but the ActorType actorCode will need to declare XDS in its profile.
 * option should match the option part of the test collections.txt file. If the option is not specified, Required is assumed, but the options list needs to contain the Required option.
 * This aligns with the Conformance Tool configuration file
 * ConfTestsTabs.xml which lives in toolkitx.
 *
 * So the big picture is that the actorCode/profile/option is now, in some cases, actually the profile/actor/option type.
 */
public class ActorType  {
    private String name;
    private List<String> altNames;
    private String shortName;
    private List<TransactionType> transactionTypes; // TransactionTypes this actor can receive
    private boolean showInConfig;
    private String actorsFileLabel;
    private String simulatorFactoryName;
    private String simulatorClassName;
    private List<TransactionType> httpTransactionTypes;
    private String httpSimulatorClassName;
    private boolean isFhir;
    private List<String> proxyTransforms;
    private String actorCode;
    private IheItiProfile profile;
    /**
     * Conformance Test Options
     */
    private List<OptionType> options;

//    Map asMap() {
//        def x = [:]
//        x.name = name
//        x.altNames = altNames
//        x.shortName = shortName
//        x.transactionTypes = transactionTypes
//        x.showInConfig = showInConfig
//        x.actorsFileLabel = actorsFileLabel
//        x.simulatorFactoryName = simulatorFactoryName
//        x.simulatorClassName = simulatorClassName
//        x.httpTransactionTypes = httpTransactionTypes
//        x.httpSimulatorClassName = httpSimulatorClassName
//        x.isFhir = isFhir
//        x.proxyTransforms = proxyTransforms
//        x.actorCode = actorCode
//
//        x
//    }

    public static List<ActorType> types = new ArrayList<>();

    private static void init(File ec) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        File typesDir = new File(new File(ec, "types"), "actors");
        File[] files = typesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith("json")) continue;
                ActorType type = objectMapper.readValue(file, ActorType.class);
                types.add(type);
            }
        }
    }

     public String getActor()  {
        return shortName;
   }

    public boolean isProxy() {
        if (proxyTransforms == null) return false;
        if (proxyTransforms.size() == 0) return false;
        return true;
   }

    public boolean isFhir() { return isFhir; }

    public String getSimulatorFactoryName() { return simulatorFactoryName; }

    public boolean showInConfig() {
        return showInConfig;
    }

    public String getSimulatorClassName() { return simulatorClassName; }

    public String getHttpSimulatorClassName() { return httpSimulatorClassName; }

    public String getActorsFileLabel() {
        return actorsFileLabel;
    }

    public static  List<String> getActorNames() {
        List<String> names = new ArrayList<>();

        for (ActorType a : types)
            names.add(a.name);

        return names;
    }

    public static  List<String> getActorNamesForConfigurationDisplays() {
        List<String> names = new ArrayList<String>();

        for (ActorType a : types)
            if (a.showInConfig())
                names.add(a.name);

        return names;
    }

    /**
     * Within toolkit, each TransactionType maps to a unique ActorType
     * (as receiver of the actor). To make this work, actor
     * names are customized to make this mapping unique.  This goes
     * beyond the definition in the TF.
     *
     * NOT SAFE - THERE CAN BE MULTIPLE ACTORS DECLARING SINGLE
     * TRANSACTIONTYPE
     *
     * @param tt
     * @return
     */
    public static  ActorType getActorType(TransactionType tt) {
        if (tt == null)
            return null;
        for (ActorType at : types) {
            if (at.hasTransaction(tt))
                return at;
        }
        return null;
    }

    public static Set<ActorType> getActorTypes(TransactionType tt) {
        Set<ActorType> types = new HashSet<>();
        if (tt == null)
            return types;
        for (ActorType at : types) {
            if (at.hasTransaction(tt))
                types.add(at);
        }
        return types;
    }

    public static  Set<ActorType> getAllActorTypes() {
        Set<ActorType> types = new HashSet<>();
        for (ActorType at : types) {
                types.add(at);
        }
        return types;
    }

    public static  ActorType findActor(String name) {
        if (name == null)
            return null;

        for (ActorType actor : types) {
            if (actor.name.equals(name))
                return actor;
            if (actor.shortName.equals(name))
                return actor;
            if (actor.altNames.contains(name))
                return actor;
            if (actor.actorCode != null && actor.actorCode.equals(name))
                return actor;
        }
        return null;
    }

    /**
     * Finds actor type by its test collection code.
     * @param tcCode
     * @return
     */
    public static  ActorType findActorByTcCode(String tcCode) {
        if (tcCode == null)
            return null;

        for (ActorType actor : types) {
            if (actor.getActorCode().equalsIgnoreCase(tcCode)) return actor;
        }
        return null;
    }

    public static  TransactionType find(String receivingActorStr, String transString) {
        if (receivingActorStr == null || transString == null) return null;

        ActorType a = findActor(receivingActorStr);
        return a.getTransaction(transString);
    }

    /**
     * Return TransactionType for passed actor name.
    * @param name of actor, matched to TransactionType short name, name,
    * or id. Both SOAP and Http transactions are searched
    * @return TransactionType for this name, or null if no match found.
    */
    public TransactionType getTransaction(String name) {
        for (TransactionType tt : transactionTypes) {
            if (tt.getShortName().equals(name)) return tt;
            if (tt.getName().equals(name)) return tt;
            if (tt.getId().equals(name)) return tt;
        }
        for (TransactionType tt : httpTransactionTypes) {
           if (tt.getShortName().equals(name)) return tt;
           if (tt.getName().equals(name)) return tt;
           if (tt.getId().equals(name)) return tt;
       }
        return null;
    }


    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append(name).append(" [");
        for (TransactionType tt : transactionTypes)
            buf.append(tt).append(",");
        for (TransactionType tt : httpTransactionTypes)
           buf.append(tt).append(",");
        buf.append("]");

        return buf.toString();
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public List<TransactionType> getTransactions() {
        return transactionTypes;
    }

    public  List<TransactionType> getHTTPTransactions() { return httpTransactionTypes; }

    public boolean hasTransaction(TransactionType transType) {
      for (TransactionType transType2 : transactionTypes) {
         if (transType2.equals(transType)) return true;
      }
         for (TransactionType transType2 : httpTransactionTypes) {
            if (transType2.equals(transType)) return true;
         }
      return false;
   }


    public  boolean equals(ActorType at) {
        try {
            return name.equals(at.name);
        } catch (Exception e) {
        }
        return false;
    }

    public List<String> getProxyTransforms() {
        return proxyTransforms;
    }

    public String getActorCode() {
       if (Constants.USE_SHORTNAME == actorCode)
           return shortName;
       else
            return actorCode;
    }


    private static class Constants {
        static final String USE_SHORTNAME = null;
    }
}
