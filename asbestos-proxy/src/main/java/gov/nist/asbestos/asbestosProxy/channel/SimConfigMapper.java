package gov.nist.asbestos.asbestosProxy.channel;


import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SimConfigMapper {
    private static Logger log = Logger.getLogger(SimConfigMapper.class);
    private Map<String, String> parms;

    SimConfigMapper(Map<String, String> parms) {
        this.parms = parms;
    }

    public ChannelConfig build() {
        ChannelConfig simConfig;
        Map<String, String> extra = new HashMap<>();

        int maxExtra = 50;
        while (maxExtra > 0) {
            try {
                simConfig = new ChannelConfig(parms)
                simConfig.extensions = extra
                return simConfig
            } catch (Throwable t) {
                def msg = t.message
                if (msg.startsWith('No such property:')) {
                    msg = msg.substring(msg.indexOf(':') + 1).trim()
                    int firstSpace = msg.indexOf(' ')
                    String propName = msg.substring(0, firstSpace).trim()
                        extra[propName] = parms[propName]
                        parms.remove(propName)
                        log.debug "Moving ${propName} to extenions"
                    maxExtra--
                    continue
                }
                throw t
            }
        }
        assert true : "SimConfigMapper: Cannot load ${parms}"
    }
}
