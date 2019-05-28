package gov.nist.asbestos.simapi.simCommon;

import gov.nist.asbestos.simapi.tk.actors.ActorType;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 *
 */
public class RuntimeManager {

    private static String getActorSimClassName(SimId simId) {
        Objects.requireNonNull(simId);
        SimulatorConfig config = GenericSimulatorFactory.getSimConfig(simId);
        String actorTypeName = config.getActorType();
        ActorType actorType = ActorType.findActor(actorTypeName);
        return actorType.getSimulatorClassName();
    }

    static BaseActorSimulator getSimulatorRuntime(SimId simId) {
        try {
            String actorSimClassName = getActorSimClassName(simId);
            if (StringUtils.isBlank(actorSimClassName))
                return null;
            Class<?> clas = Class.forName(actorSimClassName);

            // find correct constructor - no parameters
            Constructor<?>[] constructors = clas.getConstructors();
            Constructor<?> constructor = null;
            for (int i = 0; i < constructors.length; i++) {
                Constructor<?> cons = constructors[i];
                Class<?>[] parmTypes = cons.getParameterTypes();
                if (parmTypes.length != 0) continue;
                constructor = cons;
            }
            if (constructor == null)
                throw new Exception("Cannot find no-argument constructor for " + actorSimClassName);
            Object obj = constructor.newInstance();
            if (!(obj instanceof BaseActorSimulator))
                throw new Exception("Received message for actor type " + actorSimClassName + " which has a handler/simulator that does not extend BaseActorSimulator");
            return (BaseActorSimulator) obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
