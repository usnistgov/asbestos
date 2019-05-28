package gov.nist.asbestos.simapi.simCommon;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger

/**
 *
 */
 class SimulatorConfigIoJackson implements SimulatorConfigIo{
        static Logger logger = Logger.getLogger(SimulatorConfigIoJackson.class);

         void save(SimulatorConfig sc, String filename)  {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(new File(filename), sc);
        }

         SimulatorConfig restoreSimulator(String filename) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(new File(filename), SimulatorConfig.class);
        }

}
